%------------------------------------------------------------------------
% FILE          : ranlp.cls
% DATE          : 2007-03-03
% LaTeX version : LaTeX2e
% AUTHOR        : Nicolas Nicolov ({first}_{surname} at yahoo com)
%------------------------------------------------------------------------

\LoadClassWithOptions{article}

\usepackage{epsfig}
%\usepackage[dvips]{graphicx}

\usepackage{amssymb}
\usepackage{amsmath}
\usepackage{amsfonts}

\usepackage{ranlpmacros}


%------------------------------------------------------------------------
% Physical page layout 
%------------------------------------------------------------------------

% A4 sheet = 210mm x 297mm / 8.25in x 11.625in

\setlength\topmargin{-5.2mm} 
\setlength\oddsidemargin{-5.4mm}
\setlength\evensidemargin{-5.4mm}
\setlength\textheight{247mm}
\setlength\textwidth{170mm}	
\setlength\columnsep{0.2in}  
\newlength\titlebox \setlength\titlebox{1.75in}
\setlength\headheight{0pt}   \setlength\headsep{0pt}
% \setlength\footheight{0pt}   
\setlength\footskip{30pt}
\flushbottom \twocolumn \sloppy

%------------------------------------------------------------------------

\renewenvironment{abstract}
                 {\centerline{\large\bf Abstract}\vspace*{-1ex}
                  \begin{small}\begin{quote}}
                 {\par\end{quote}\end{small}\vskip 1ex}

%------------------------------------------------------------------------
% Captions are in italic
%------------------------------------------------------------------------

\long\def\@makecaption#1#2{
   \vskip 10pt
   \setbox\@tempboxa\hbox{\textbf{#1:} \textit{#2}}
   \ifdim \wd\@tempboxa >\hsize      % IF longer than one line:
       \textbf{#1:} \textit{#2}\par  %   THEN set as ordinary paragraph.
     \else                           %   ELSE  center.
       \hbox to\hsize{\hfil\box\@tempboxa\hfil}
   \fi}

%------------------------------------------------------------------------

%%% Reduce distance between baselines:

\renewcommand{\baselinestretch}{0.9}


%--- NN: CHANGING HOW THE FOOTNOTE LOOKS: -------------------------------
% Put indentation between the footnote marker and the text of the footnote.

\makeatletter
\long\def\@makefntext#1{\@setpar{\@@par\@tempdima \hsize 
  \advance\@tempdima-10pt\parshape \@ne 10pt \@tempdima}\par
  \parindent 1em\noindent \hbox to \z@{\hss$^{\@thefnmark}\ $}#1}
\makeatother
%------------------------------------------------------------------------
