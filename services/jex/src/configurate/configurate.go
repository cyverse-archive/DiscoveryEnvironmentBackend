package configurate

import (
	"io/ioutil"
	"os"

	"github.com/olebedev/config"
)

var (
	//C is a global *config.Config
	C *config.Config
)

// Init initializes the underlying config.
func Init(path string) error {
	f, err := os.Open(path)
	if err != nil {
		return err
	}
	contents, err := ioutil.ReadAll(f)
	if err != nil {
		return err
	}
	C, err = config.ParseYaml(string(contents))
	return err
}
