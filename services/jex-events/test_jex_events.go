package main

import "testing"

// TestExtractCondorID test the ExtractCondorID function
func TestExtractCondorID(t *testing.T) {
	input := `(100.0.0)`
	extracted := ExtractCondorID(input)
	if extracted != "100" {
		t.Error("The extracted condor ID was not '100'")
	}
	input2 := `100.0.0`
	extracted2 := ExtractCondorID(input2)
	if extracted2 != "" {
		t.Error("The extracted condor ID was not blank")
	}
}
