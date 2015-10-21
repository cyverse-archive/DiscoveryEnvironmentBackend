package configurate

import "testing"

func configurator() error {
	path := "../test/test_config.yaml"
	return Init(path)
}

func TestNew(t *testing.T) {
	err := configurator()
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	if C == nil {
		t.Errorf("configurate.New() returned nil")
	}
}

func TestAMQPConfig(t *testing.T) {
	err := configurator()
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	actual, err := C.String("amqp.uri")
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	expected := "amqp://guest:guest@192.168.99.100:5672/"
	if actual != expected {
		t.Errorf("The amqp.uri was %s instead of %s", actual, expected)
	}
}

// func TestValid(t *testing.T) {
// 	cfg, err := configurator()
// 	if err != nil {
// 		t.Error(err)
// 		t.Fail()
// 	}
// 	if !cfg.Valid() {
// 		t.Errorf("configurate.Valid() return false")
// 	}
// }
