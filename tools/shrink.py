#!/usr/bin/python2.4

"""Shrink dictionary."""

import sys

previous_word = '---'

def common_letters(word_a, word_b):
  """ Returns how many of the first letters of 'a'
  are also the first letters of 'b'
  """
  count = 0
  if len(word_a) > len(word_b):
    size = len(word_b)
  else:
    size = len(word_a)
  for i in xrange(0, size-2):
    if word_a[i] == word_b[i]:
      count += 1
    else:
      return count
  return count

for word in sys.stdin.readlines():
  word = word.strip()
  commons = common_letters(previous_word, word)
  print '%d%s' % (commons, word[commons:])
  previous_word = word
