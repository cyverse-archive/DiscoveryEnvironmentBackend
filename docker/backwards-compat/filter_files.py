#!/bin/env python

import sys

first_file = sys.argv[1]
second_file = sys.argv[2]

first = open(first_file, "r")
second = open(second_file, "r")

first_lines = [s.strip() for s in first.readlines()]
second_lines = [s.strip() for s in second.readlines()]

first_set = set(first_lines)
second_set = set(second_lines)

diff = first_set ^ second_set

for i in diff:
    print i
