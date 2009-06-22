#!/usr/bin/python2.4

"""Loads infl.txt and objectivises(?) the words."""

import sys

noun_plurals = []
verb_s = []

class Word(object):
  """A word in the inflections list.

  Attributes:
    word: str, the word
    type: type of word ('V', 'N')
    plural_s: plural form
  """
  def __init__(self, line):
    line = line.strip()
    splitted = line.split()
    self.word = splitted[0]
    self.type = splitted[1][0]
    self.plural_s = []
    self.verb_s = []
    if self.type == 'N':
      forms = ' '.join(splitted[2:]).split(', ')
      for w in forms:
        if w.endswith('s') and w != self.word:
          noun_plurals.append(w)
    elif self.type == 'V':
      forms = ' '.join(splitted[2:]).split(' | ')
      for word in forms:
        if word[-1] == '?':
          word = word[:-1]
        if word.endswith('s'):
          verb_s.append(word)

class Words(list):
  def __init__(self, filename=None):
    if filename: 
      fd = file(filename)
      for line in fd:
        word = Word(line)

if __name__ == '__main__':
  w = Words(sys.argv[1])
  for word in noun_plurals:
    if word not in verb_s:
      print word

