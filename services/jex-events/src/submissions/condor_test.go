package submissions

import (
	"fmt"
	"os"
	"path"
	"strings"
	"testing"
)

func TestGenerateCondorSubmit(t *testing.T) {
	s := inittests(t)
	actual, err := GenerateCondorSubmit(s)
	if err != nil {
		t.Error(err)
	}
	expected := `universe = vanilla
executable = /bin/bash
rank = mips
arguments = "iplant.sh"
output = script-output.log
error = script-error.log
request_disk = 0
+IpcUuid = "07b04ce2-7757-4b21-9e15-0b4c2f44be26""
+IpcJobId = "generated_script"
+IpcUsername = "test_this_is_a_test"
concurrency_limits = "test_this_is_a_test"
+IpcExe = "wc_wrapper.sh"
+IpcExePath = "/usr/local3/bin/wc_tool-1.00"
should_transfer_files = YES
transfer_input_files = iplant.sh,irods-config,iplant.cmd
transfer_output_files = logs/de-transfer-trigger.log,logs/output-last-stdout,logs/output-last-stderr
when_to_transfer_output = ON_EXIT_OR_EVICT
notification = NEVER
queue
`
	if actual != expected {
		t.Errorf("GenerateCondorSubmit() returned:\n\n%s\n\ninstead of:\n\n%s", actual, expected)
	}
	s.Group = "foo"
	actual, err = GenerateCondorSubmit(s)
	if err != nil {
		t.Error(err)
	}
	expected = `universe = vanilla
executable = /bin/bash
rank = mips
arguments = "iplant.sh"
output = script-output.log
error = script-error.log
request_disk = 0
+IpcUuid = "07b04ce2-7757-4b21-9e15-0b4c2f44be26""
+IpcJobId = "generated_script"
+IpcUsername = "test_this_is_a_test"
+AccountingGroup = "foo.test_this_is_a_test"
concurrency_limits = "test_this_is_a_test"
+IpcExe = "wc_wrapper.sh"
+IpcExePath = "/usr/local3/bin/wc_tool-1.00"
should_transfer_files = YES
transfer_input_files = iplant.sh,irods-config,iplant.cmd
transfer_output_files = logs/de-transfer-trigger.log,logs/output-last-stdout,logs/output-last-stderr
when_to_transfer_output = ON_EXIT_OR_EVICT
notification = NEVER
queue
`
	if actual != expected {
		t.Errorf("GenerateCondorSubmit() returned:\n\n%s\n\ninstead of:\n\n%s", actual, expected)
	}
	_inittests(t, false)
}

func TestGenerateIplantScript(t *testing.T) {
	s := inittests(t)
	s.NowDate = "test"
	s.Steps[0].Environment = StepEnvironment{}
	str, err := GenerateIplantScript(s)
	if err != nil {
		t.Error(err)
	}
	actual := strings.Split(str, "\n")
	expected := strings.Split(`#!/bin/bash

set -x

readonly IPLANT_USER=test_this_is_a_test
export IPLANT_USER
readonly IPLANT_EXECUTION_ID=07b04ce2-7757-4b21-9e15-0b4c2f44be26
export IPLANT_EXECUTION_ID
export SCRIPT_LOCATION=${BASH_SOURCE}
EXITSTATUS=0

if [ -e /data2 ]; then ls /data2; fi

mkdir -p logs

if [ ! "$?" -eq "0"]; then
	EXITSTATUS=1
	exit $EXITSTATUS
fi

ls -al > logs/de-transfer-trigger.log

if [ ! "$?" -eq "0"]; then
	EXITSTATUS=1
	exit $EXITSTATUS
fi

if [ -e "iplant.sh" ]; then
	mv iplant.sh logs/
fi

if [ -e "iplant.cmd" ]; then
	mv iplant.cmd logs/
fi

docker pull vf-name1:vf-tag1
docker pull vf-name2:vf-tag2
docker pull gims.iplantcollaborative.org:5000/backwards-compat:latest

docker create -v /host/path1:/container/path1:ro --name vf-prefix1-07b04ce2-7757-4b21-9e15-0b4c2f44be26 vf-name1:vf-tag1
if [ ! "$?" -eq "0"]; then
	EXITSTATUS=1
	exit $EXITSTATUS
fi

docker create -v /host/path2:/container/path2:ro --name vf-prefix2-07b04ce2-7757-4b21-9e15-0b4c2f44be26 vf-name2:vf-tag2
if [ ! "$?" -eq "0"]; then
	EXITSTATUS=1
	exit $EXITSTATUS
fi

docker run --rm -a stdout -a stderr -v $(pwd):/de-app-work -w /de-app-work discoenv/porklock:test get --user test_this_is_a_test --source '/iplant/home/wregglej/Acer-tree.txt' --config irods-config -m 'attr1,value1,unit1' -m 'attr2,value2,unit2' -m 'ipc-analysis-id,c7f05682-23c8-4182-b9a2-e09650a5f49b,UUID' -m 'ipc-execution-id,07b04ce2-7757-4b21-9e15-0b4c2f44be26,UUID' >1 logs/logs-stdout-input-0 >2 logs/logs-stderr-input-0
if [ ! "$?" -eq "0"]; then
	EXITSTATUS=1
	exit $EXITSTATUS
fi

docker run --rm -e IPLANT_USER -e IPLANT_EXECUTION_ID -v /usr/local2/:/usr/local2 -v /usr/local3/:/usr/local3/ -v /data2/:/data2/ -v $(pwd):/work -v /host/path1:/container/path1 -v /container/path2 --device=/host/path1:/container/path1 --device=/host/path2:/container/path2 --volumes-from=07b04ce2-7757-4b21-9e15-0b4c2f44be26-vf-prefix1 --volumes-from=07b04ce2-7757-4b21-9e15-0b4c2f44be26-vf-prefix2 --name test-name -w /work --memory=2048M --cpu-shares=2048 --net=none --entrypoint=/bin/true gims.iplantcollaborative.org:5000/backwards-compat:test /usr/local3/bin/wc_tool-1.00/wc_wrapper.sh param1 'Acer-tree.txt' param0 'wc_out.txt' >1 /path/to/stdout >2 /path/to/stderr
if [ ! "$?" -eq "0"]; then
	EXITSTATUS=1
	exit $EXITSTATUS
fi

docker run --rm -v $(pwd):/de-app-work -w /de-app-work discoenv/porklock:test put --user test_this_is_a_test --config irods-config --destination '/iplant/home/wregglej/analyses/Word_Count_analysis1-2015-09-17-21-42-20.9/Word_Count_analysis1__-test' -m 'attr1,value1,unit1' -m 'attr2,value2,unit2' -m 'ipc-analysis-id,c7f05682-23c8-4182-b9a2-e09650a5f49b,UUID' -m 'ipc-execution-id,07b04ce2-7757-4b21-9e15-0b4c2f44be26,UUID' --exclude foo,bar,baz,blippy >1 logs/logs-stdout-output >2 logs/logs-stderr-output
if [ ! "$?" -eq "0"]; then
	EXITSTATUS=1
	exit $EXITSTATUS
fi
docker rm vf-prefix1-07b04ce2-7757-4b21-9e15-0b4c2f44be26
docker rm vf-prefix2-07b04ce2-7757-4b21-9e15-0b4c2f44be26

hostname
ps aux
echo -----
for i in $(ls logs); do
    echo logs/$i
    cat logs/$i
    echo -----\
done
exit $EXITSTATUS
`, "\n")
	if len(actual) != len(expected) {
		t.Errorf("GenerateIplantScript() returned %d lines rather than %d", len(actual), len(expected))
		t.Fail()
	}
	for idx, e := range expected {
		if actual[idx] != e {
			t.Errorf("\nActual:\n%s\nExpected:\n%s\n", actual[idx], e)
		}
	}
	_inittests(t, false)
}

func TestCreateSubmissionDirectory(t *testing.T) {
	s := inittests(t)
	cfg.CondorLogPath = ""
	dir, err := CreateSubmissionDirectory(s)
	if err != nil {
		t.Error(err)
	}
	_, err = os.Stat(dir)
	if err != nil {
		t.Error(err)
	}
	parent := path.Join(cfg.CondorLogPath, s.Username)
	err = os.RemoveAll(parent)
	if err != nil {
		t.Error(err)
	}
	_inittests(t, false)
}

func TestCreateSubmissionFiles(t *testing.T) {
	s := inittests(t)
	cfg.CondorLogPath = ""
	dir, err := CreateSubmissionDirectory(s)
	if err != nil {
		t.Error(err)
	}
	cmd, sh, err := CreateSubmissionFiles(dir, s, cfg)
	if err != nil {
		t.Error(err)
	}
	_, err = os.Stat(cmd)
	if err != nil {
		t.Error(err)
	}
	_, err = os.Stat(sh)
	if err != nil {
		t.Error(err)
	}
	irodsPath := path.Join(path.Dir(cmd), "irods-config")
	_, err = os.Stat(irodsPath)
	if err != nil {
		t.Error(err)
	}
	parent := path.Join(cfg.CondorLogPath, s.Username)
	err = os.RemoveAll(parent)
	if err != nil {
		t.Error(err)
	}
	_inittests(t, false)
}

func TestCondorSubmit(t *testing.T) {
	s := inittests(t)
	PATH := fmt.Sprintf(".:%s", os.Getenv("PATH"))
	err := os.Setenv("PATH", PATH)
	if err != nil {
		t.Error(err)
	}
	cfg.CondorLogPath = ""
	dir, err := CreateSubmissionDirectory(s)
	if err != nil {
		t.Error(err)
	}
	cmd, sh, err := CreateSubmissionFiles(dir, s, cfg)
	if err != nil {
		t.Error(err)
	}
	actual, err := CondorSubmit(cmd, sh, s)
	if err != nil {
		t.Error(err)
	}
	expected := "10000"
	if actual != expected {
		t.Errorf("CondorSubmit() returned %s instead of %s", actual, expected)
	}
	parent := path.Join(cfg.CondorLogPath, s.Username)
	err = os.RemoveAll(parent)
	if err != nil {
		t.Error(err)
	}
}
