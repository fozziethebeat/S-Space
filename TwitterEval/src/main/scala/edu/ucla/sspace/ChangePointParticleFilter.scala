package edu.ucla.sspace

import breeze.numerics.{digamma, lgamma}
import breeze.linalg.DenseVector
import breeze.stats.distributions.NegativeBinomial

import math.{abs, exp, log, log10, pow}

class ChangePointParticleFilter {

    type Particle = (Long, // Time of most recent change point
                     Double, // w weight of particle
                     DenseVector[Double] // beta weight of particle
                    )

    // Fixed hyper parameter setting the number of particles to use.
    val maxParticles = 100
    // Threshold for dropping particles.  Since we have at most 100 particles, at the worst case, they all have the same normalized weight
    // of 0.01, so this will throw out any below that threshold.
    val threshold = 0.01
    val omega = .1

    // Initial values for parameters that go into theta.
    val r_0 = 3
    val rho_0 = 0.5
    val lambda_0 = 0.5
    val beta_0 = 100d

    var theta = formTheta( (r_0, rho_0), (lambda_0, beta_0) )

    // The current Negative Binomial distribution using parameters in theta.
    var h = newNegativeBinomialDist
    var particleList = List[Particle]( (0l, 1d, DenseVector.zeros[Double](4)) )
    var lastTime = 0l

    def newNegativeBinomialDist = new NegativeBinomial(theta(0), theta(1))

    def negativeBinomialGradiant(x: Long) = {
        val (r, rho) = (theta(0), theta(1))
        val gamma_x_1 = gamma(x+1)
        val gamma_r = gamma(r)
        val gamma_x_r = gamma(x+r)
        val to_r = pow(rho, x) / gamma_x_1 * pow(1-rho, r) * gamma_x_r / gamma_r *
                   (digamma(x+r) + log(1-rho) - 1 / (gamma_r*digamma(r)))
        val to_rho = gamma_x_r / (gamma_x_1 * gamma_r) * pow(rho, x-1) * pow(1-rho, r-1) *
                     ( (1-rho) * x - rho * r )
        (to_r, to_rho)
    }

    def p(t1: Long, t2: Long) = pow(theta(2), abs(t1 - t2) / theta(3))

    def f(t: Long, currentChangePoint: Long, previousChangePoint: Long) = {
        val delta = (t - previousChangePoint).toInt
        if (currentChangePoint == previousChangePoint)
            (1 - h(delta - 1)) / (1 - h(delta - 2))
        else 
            (h(delta-1) - h(delta-2)) / (1 - h(delta-2))
    }

    def f_gradiant(t: Long, currentChangePoint: Long, previousChangePoint: Long) = {
        val delta = (t - previousChangePoint).toInt
        if (currentChangePoint == previousChangePoint) {
            val h_1 = h(delta-1)
            val scale_1 = 1 / (1-h_1) * log10(1-h_1)
            val params_1 = negativeBinomialGradiant(delta-1)

            val h_2 = h(delta-2)
            val scale_2 = 1 / (1-h_2) * log10(1-h_2)
            val params_2 = negativeBinomialGradiant(delta-2)

            (scale_1 * params_1._1 - scale_2 * params_2._1,
             scale_1 * params_1._2 - scale_2 * params_2._2)
        } else  {
            val h_1 = h(delta-1)
            val h_2 = h(delta-2)

            val scale_1 = 1 / (h_1 - h_2) * log10(h_1 - h_2)
            val scale_2 = 1 / (1-h_2) * log10(1-h_2)

            val params_1 = negativeBinomialGradiant(delta-1)
            val params_2 = negativeBinomialGradiant(delta-2)

            (scale_1 * (params_1._1 - params_2._1) - scale_2 * params_2._1,
             scale_1 * (params_1._2 - params_2._2) - scale_2 * params_2._2)
        }
    }

    def g(currentChangePoint: Long, currentTime: Long, previousTime: Long) =
        if (currentChangePoint == previousTime)
            p(previousTime, currentTime)
        else
            p(currentChangePoint, currentTime) / p(currentChangePoint, previousTime)

    def g_gradiant(currentChangePoint: Long, currentTime: Long, previousTime: Long) = {
        val delta = if (currentChangePoint == previousTime)
            abs(currentTime - previousTime)
        else
            abs(currentChangePoint - currentTime) - abs(currentChangePoint - previousTime)
        val (lambda, beta) = (theta(2), theta(3))
        val beta_2 = beta * beta
        val delta_beta = delta / beta
        val lambda_pow = pow(lambda, delta_beta)
        // Return the gradiant which is the partial derivative of g with respect to lambda and beta.
        (delta_beta * lambda_pow, -1*log(pow(lambda, delta))*lambda_pow/beta_2)
    }
    
    def formTheta( nb_params: (Double, Double), e_params: (Double, Double)) = 
        DenseVector[Double](nb_params._1, nb_params._2, e_params._1, e_params._2)

    def process(time: Long, value: Double) {
        // For every particle, compute the new weight based on the current data point.  In addition to this, evaluate the f function when
        // considering the newest possible change point and store it.  That value will be used twice so we can save some computation by
        // doing it once and storing it.
        var updatedParticles = particleList.map{ case (c_t, w_t, beta_t) => 
            (c_t, // The last change point.
             g(c_t, time, lastTime) * f(time, c_t, c_t) * w_t, // The updated weight.
             f(time, lastTime, c_t)*w_t, // The f function evaluted with the latest change point
             beta_t // The theta weight vector stays the same for now.
            )
        }
        // Evaluate the g function using the proposed change point.
        val proposedG = g(lastTime, time, lastTime)
        // Collapse the likelihood of making a new change point 
        val proposedWeight = proposedG * updatedParticles.map(_._3).sum 
        // Compute the alpha vector for the new change point.  This involves collapsing the f function evaluated at this change point and
        // the gradiants computed at this change point.
        val proposedAlpha:DenseVector[Double] = 
            updatedParticles.map{ case(c_t, w_t, f_t, beta_t) =>
                (formTheta( g_gradiant(lastTime, time, lastTime), f_gradiant(time, lastTime, c_t)) + beta_t) :* f_t
            }.reduce(_+_) :* proposedG

        // Compute the new, updated particle list as real particles.  This also includes the newest change point at the tail end.
        val newParticleList = updatedParticles.map{ case(c_t, w_t, f_t, beta_t) =>
            (c_t, w_t, formTheta( g_gradiant(c_t, time, lastTime), f_gradiant(time, c_t, c_t) ) :* w_t )} :+
            (lastTime, proposedWeight, proposedAlpha)


        // Now update theta using the weighted alpha vectors computed.
        theta += (newParticleList.map(_._3).reduce(_+_) / newParticleList.map(_._2).sum) * omega

        val selectedParticles = selectParticles(newParticleList)

        // Compute the alpha sum once.
        val alphaSum = selectedParticles.map(_._3).reduce(_+_)
        // Compute the weight sum once.
        val weightSum = selectedParticles.map(_._2).sum
        // Now transform each selected particle to contain the actual updated beta weight vector and store this in particleList
        particleList = selectedParticles.map{ case(c_t, w_t, alpha_t) =>
            (c_t, w_t, (alpha_t / weightSum - alphaSum / weightSum * w_t) / w_t)
        }
        // Update the negative binomial distribution
        h = newNegativeBinomialDist
        // Update the last time observed.
        lastTime = time
    }

    def selectParticles(particles: List[Particle]) = {
        if (particles.size < maxParticles)
            particles
        else {
            val weightSum = particles.map(_._2).sum
            // Use a shitty selection method for now where we just drop any particles below some threshold.
            // It's possible that this saves no partciles. FIX THAT SHIT.
            particles.filter( _._2 / weightSum >= threshold)
        }
    }

    def gamma(x: Double) = exp(lgamma(x))
}
