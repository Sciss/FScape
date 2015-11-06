val c = TCP.Client(localhost -> 18003)
c.connect()
c ! osc.Message("/main", "query", 666, "version")
c.dumpIn()
c ! osc.Message("/doc", "query", 667, "count")
c ! osc.Message("/doc", "new", "ChangeGain")
c ! osc.Message("/doc", "open", "/home/hhrutz/Music/work/hp360.fsc", 1)
c ! osc.Message("/doc/active", "close")
c ! osc.Message("/doc/index/1", "activate")
c ! osc.Message("/doc/index/1", "start") // XXX
c ! osc.Message("/doc/index/1", "query", 668, "file") // XXX

c ! osc.Message("/doc/id/1", "close")
c ! osc.Message("/doc/id/2", "activate")

c ! osc.Message("/doc/id/3", "query", 1, "name", "file",
 "process", "running", "progression", "error")


c ! osc.Message("/doc/id/3", "query", 1, "running", "progression", "error")
c ! osc.Message("/doc", "query", 2, "count")
c ! osc.Message("/doc/index/0", "close")

c ! osc.Message("/main", "quit")
