#!/bin/env python

import os
import sys
import yum

from optparse import OptionParser

yb = yum.YumBase()

def repo_exists(repo_name):
    return repo_name in [r.id for r in yb.repos.findRepos('*')]

def list_packages(repo_name, dupes=True):
    yb.repos.disableRepo('*')
    yb.repos.enableRepo(repo_name)

    for pkg in sorted(yb.doPackageLists(showdups=dupes).available):
        print str(pkg) + ".rpm"

def parse_args(args=sys.argv):
    parser = OptionParser(usage="usage: %prog [options] yum-repo")
    parser.add_option("-a", "--all", action="store_true", dest="list_all")
    return parser.parse_args(args)

if __name__ == "__main__":
    options, args = parse_args()

    if len(args) > 2:
        print "Only a single repository is supported."
        sys.exit(1)

    if len(args) < 2:
        print "A repository name must be provided."
        sys.exit(1)

    repo_name = args[1]

    if not repo_exists(repo_name):
        print "That repo doesn't exist"
        sys.exit(1)
    else:
        list_packages(repo_name, dupes=options.list_all)
        sys.exit(0)
