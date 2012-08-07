import time

junkTokens = frozenset(["#", "htt"])

def removeLineBreaks(s):
    return s.replace("\n", " ").replace("\r", " ")
def removeHttp(s): 
    return " ".join([ t for t in s.split() if not t.startswith("http")])
def removeOddities(s):
    return " ".join([ t for t in s.split() if t not in junkTokens])
def convertTime(created_at):
    return time.mktime(time.strptime(created_at,'%a %b %d %H:%M:%S +0000 %Y'))
