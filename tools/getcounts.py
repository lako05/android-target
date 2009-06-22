#!/usr/bin/python2.4

import sys

countfile = file(sys.argv[1])
wordwanted = sys.argv[2].upper()

while True:
  wordstats = countfile.read(18)
  if not wordstats:
    break
  word = ''.join(wordstats[:9])
  if word != wordwanted:
    continue
  print '%s:' % word,
  for i in xrange(9, 18):
    print ' %d' % ord(wordstats[i]),

