#!/bin/sh

#https://stackoverflow.com/questions/1011985/line-endings-messed-up-in-git-how-to-track-changes-from-another-branch-after-a

# git filter-branch --tree-filter '~/Scripts/fix-line-endings.sh' -- --all
find . -type f -a \( -name '*.tpl' -o -name '*.php' -o -name '*.js' -o -name '*.css' -o -name '*.sh' -o -name '*.txt' -iname '*.html' \) | xargs fromdos