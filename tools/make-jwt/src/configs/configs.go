package configs

import (
	"bufio"
	"fmt"
	"log"
	"os"
	"regexp"
	"strings"
)

func isSignificantLine(line string) bool {
	matched, err := regexp.MatchString("^\\s*(?:#|$)", line)
	if err != nil {
		log.Fatal(err)
	}
	return !matched
}

type ParamFile struct {
	path    string
	file    *os.File
	scanner *bufio.Scanner
}

// NewParamFile attempts to create a new ParamFile structure for the file at the given path. If
// the file exists, is a regular file and can be opened then the new ParamFile structure is
// returned. Otherwise, an error is returned.
func NewParamFile(path string) (*ParamFile, error) {

	// Verify that the file is a regular file.
	fileInfo, err := os.Stat(path)
	if err != nil {
		return nil, err
	}
	if fileInfo.IsDir() {
		return nil, fmt.Errorf("%s is a directory", path)
	}

	// Open the file.
	file, err := os.Open(path)
	if err != nil {
		return nil, err
	}

	return &ParamFile{path, file, bufio.NewScanner(file)}, nil
}

// NextParam advances the ParamFile structure to the next parameter definition in the file. Blank
// lines and comments (lines beginning with a hash mark) are skipped. Returns `true` if another
// parameter definition was found in the file, `false` otherwise.
func (p *ParamFile) NextParam() bool {

	// Search for the next significant line.
	for p.scanner.Scan() {
		if isSignificantLine(p.scanner.Text()) {
			return true
		}
	}

	// If we get to this point then there were no more significant lines.
	return false
}

// ExtractParam extracts the current parameter from the file. The behavior of this function is
// undefined for ParamFile strucures for which NextParam has not been called or for which
// NextParam returned `false` the last time it was called.
func (p *ParamFile) ExtractParam() (*string, *string) {
	line := strings.TrimSpace(p.scanner.Text())
	strs := regexp.MustCompile("\\s*=\\s*").Split(line, 2)

	if len(strs) == 2 {
		return &strs[0], &strs[1]
	}
	return &strs[0], nil
}

// Close closes the parameter file.
func (p *ParamFile) Close() error {
	return p.file.Close()
}
