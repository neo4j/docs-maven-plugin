#!/bin/bash
set -e
set -x

maven2=$1
maven3=$2

: ${maven2:="mvn"}
: ${maven3:="mvn3"}

$maven3 clean install
$maven3 clean install -Ddogfood -Dfilter=true
$maven3 docs:assemble -Ddogfood -Dfilter=true
$maven3 docs:install -Ddogfood -Dfilter=true -Dtest=DocsTest
$maven2 docs:assemble -Ddogfood -Dfilter=true
$maven2 clean install -Ddogfood -Dfilter=true

