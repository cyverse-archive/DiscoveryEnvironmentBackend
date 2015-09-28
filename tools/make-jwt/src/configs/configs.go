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

func (p *ParamFile) ExtractParam() (*string, *string) {
	line := strings.TrimSpace(p.scanner.Text())
	strs := regexp.MustCompile("\\s*=\\s*").Split(line, 2)

	if len(strs) == 2 {
		return &strs[0], &strs[1]
	}
	return &strs[0], nil
}

func (p *ParamFile) Close() error {
	return p.file.Close()
}
