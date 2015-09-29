package submissions

import (
	"configurate"
	"io/ioutil"
	"logcabin"
	"os"
	"testing"
)

func JSONData() ([]byte, error) {
	f, err := os.Open("test_submission.json")
	if err != nil {
		return nil, err
	}
	c, err := ioutil.ReadAll(f)
	if err != nil {
		return nil, err
	}
	return c, err
}

var (
	s *Submission
	c = &configurate.Configuration{}
	l = logcabin.New()
)

func _inittests(t *testing.T, memoize bool) *Submission {
	if s == nil || !memoize {
		c.RunOnNFS = true
		c.NFSBase = "/path/to/base"
		c.IRODSBase = "/path/to/irodsbase"
		c.CondorLogPath = "/path/to/logs"
		c.PorklockTag = "test"
		c.FilterFiles = "foo,bar,baz,blippy"
		c.RequestDisk = "0"
		Init(c, l)
		data, err := JSONData()
		if err != nil {
			t.Error(err)
			t.Fail()
		}
		s, err = NewFromData(data)
		if err != nil {
			t.Error(err)
			t.Fail()
		}
	}
	return s
}

func inittests(t *testing.T) *Submission {
	return _inittests(t, true)
}

func TestJSONParsing(t *testing.T) {
	inittests(t)
}

func TestNaivelyQuote(t *testing.T) {
	test1 := naivelyquote("foo")
	test2 := naivelyquote("'foo'")
	test3 := naivelyquote("foo'oo")
	test4 := naivelyquote("'foo'oo'")
	test5 := naivelyquote("foo''oo")
	test6 := naivelyquote("'foo''oo'")
	test7 := naivelyquote("f'oo'oo")
	test8 := naivelyquote("'f'oo'oo'")

	if test1 != "'foo'" {
		t.Errorf("naivelyquote returned %s instead of 'foo'", test1)
	}
	if test2 != "'''foo'''" {
		t.Errorf("naivelyquote returned %s instead of '''foo'''", test2)
	}
	if test3 != "'foo''oo'" {
		t.Errorf("naivelyquote returned %s instead of 'foo''oo'", test3)
	}
	if test4 != "'''foo''oo'''" {
		t.Errorf("naivelyquote returned %s instead of '''foo''oo'''", test4)
	}
	if test5 != "'foo''''oo'" {
		t.Errorf("naivelyquote returned %s instead of 'foo''''oo'", test5)
	}
	if test6 != "'''foo''''oo'''" {
		t.Errorf("naivelyquote returned %s instead of '''foo''''oo'''", test6)
	}
	if test7 != "'f''oo''oo'" {
		t.Errorf("naivelyquote returned %s instead of 'f''oo''oo'", test7)
	}
	if test8 != "'''f''oo''oo'''" {
		t.Errorf("naivelyquote returned %s instead of '''f''oo''oo'''", test8)
	}
}

func TestQuote(t *testing.T) {
	test1 := quote("foo")
	test2 := quote("'foo'")
	test3 := quote("foo'oo")
	test4 := quote("'foo'oo'")
	test5 := quote("foo''oo")
	test6 := quote("'foo''oo'")
	test7 := quote("f'oo'oo")
	test8 := quote("'f'oo'oo'")

	if test1 != "'foo'" {
		t.Errorf("naivelyquote returned %s instead of 'foo'", test1)
	}
	if test2 != "'foo'" {
		t.Errorf("naivelyquote returned %s instead of 'foo'", test2)
	}
	if test3 != "'foo''oo'" {
		t.Errorf("naivelyquote returned %s instead of 'foo''oo'", test3)
	}
	if test4 != "'foo''oo'" {
		t.Errorf("naivelyquote returned %s instead of 'foo''oo'", test4)
	}
	if test5 != "'foo''''oo'" {
		t.Errorf("naivelyquote returned %s instead of 'foo''''oo'", test5)
	}
	if test6 != "'foo''''oo'" {
		t.Errorf("naivelyquote returned %s instead of 'foo''''oo'", test6)
	}
	if test7 != "'f''oo''oo'" {
		t.Errorf("naivelyquote returned %s instead of 'f''oo''oo'", test7)
	}
	if test8 != "'f''oo''oo'" {
		t.Errorf("naivelyquote returned %s instead of 'f''oo''oo'", test8)
	}
}
