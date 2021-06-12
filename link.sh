#!/bin/bash
build_lower=/tmp/$(uuidgen)
build_upper=/tmp/$(uuidgen)
build_workdir=/tmp/$(uuidgen)
mkdir ${build_lower} ${build_upper} ${build_workdir}
sudo mount -t overlay -o lowerdir=${build_lower},upperdir=${build_upper},workdir=${build_workdir} none ./build
