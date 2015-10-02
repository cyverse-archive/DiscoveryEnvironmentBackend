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
