package msgpack

import (
	"bytes"
	"fmt"
	"reflect"
	"testing"
)

func TestRawMessage(t *testing.T) {
	testMsg := []byte{
		fixMapLowCode | 3,
		fixStrLowCode | 4, 'k', 'e', 'y', '1',
		fixStrLowCode | 5, 'v', 'a', 'l', 'u', 'e',
		fixStrLowCode | 4, 'k', 'e', 'y', '2',
		fixArrayLowCode | 2,
		fixStrLowCode | 6, 'v', 'a', 'l', 'u', 'e', '1',
		fixStrLowCode | 6, 'v', 'a', 'l', 'u', 'e', '2',
		fixStrLowCode | 4, 'k', 'e', 'y', '3',
		fixArrayLowCode | 6,
		fixExt1Code, 0x01, 0x8f,
		fixExt2Code, 0x02, 0x01, 0x02,
		fixExt4Code, 0x03, 0x01, 0x02, 0x03, 0x04,
		fixExt8Code, 0x04, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
		fixExt16Code, 0x05, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
		0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10,
		ext8Code, 0x03, 0x01, 0x01, 0x02, 0x03,
	}

	var rawMessage RawMessage
	if err := Unmarshal(testMsg, &rawMessage); err != nil {
		t.Fatalf("Error decoding message: %s", err)
	}
	if bytes.Compare(rawMessage.raw, testMsg) != 0 {
		t.Fatalf("Wrong decode value:\n\tExpected: %x\n\tActual:   %x", testMsg, rawMessage.raw)
	}

}

type counterStruct struct {
	id          byte
	decodeCount byte
	encodeCount byte
}

type extensionTester struct {
	id byte
}

func (et *extensionTester) decodeCounterStruct(v reflect.Value, b []byte) error {
	if len(b) != 3 {
		return fmt.Errorf("Invalid length: %d", len(b))
	}
	counter := &counterStruct{
		id:          b[0],
		decodeCount: b[1] + 1,
		encodeCount: b[2],
	}
	if counter.id != et.id {
		return fmt.Errorf("Decoded unknown counter: %s", counter.id)
	}
	if !v.CanSet() {
		v.Elem().Set(reflect.ValueOf(*counter))
	} else {
		v.Set(reflect.ValueOf(counter))
	}

	return nil
}

func (et *extensionTester) encodeCounterStruct(v reflect.Value) (int, []byte, error) {
	counter, ok := v.Interface().(*counterStruct)
	if !ok {
		return 0, nil, nil
	}
	return 1, []byte{et.id, counter.decodeCount, counter.encodeCount + 1}, nil
}

func TestEncode(t *testing.T) {
	extensionTester1 := &extensionTester{1}
	config1 := NewExtensions()
	config1.SetEncoder(extensionTester1.encodeCounterStruct)
	config1.AddDecoder(1, reflect.TypeOf(&counterStruct{}), extensionTester1.decodeCounterStruct)
	extensionTester2 := &extensionTester{2}
	config2 := NewExtensions()
	config2.SetEncoder(extensionTester2.encodeCounterStruct)
	config2.AddDecoder(1, reflect.TypeOf(&counterStruct{}), extensionTester2.decodeCounterStruct)

	var encoded []byte
	{
		var counter counterStruct
		buf := bytes.NewBuffer(nil)
		encoder := NewEncoder(buf)
		encoder.AddExtensions(config1)
		if err := encoder.Encode(&counter); err != nil {
			t.Fatalf("Error encoding counter: %s", err)
		}
		encoded = buf.Bytes()
	}

	{
		var counter counterStruct
		decoder := NewDecoder(bytes.NewReader(encoded))
		decoder.AddExtensions(config1)
		if err := decoder.Decode(&counter); err != nil {
			t.Fatalf("Error decoding counter: %s\n\t%x", err, encoded)
		}
		if counter.id != extensionTester1.id {
			t.Fatalf("Unexpected id: %d (expected %d)", counter.id, extensionTester1.id)
		}
		if counter.decodeCount != 1 {
			t.Fatalf("Unexpected decode count: %d (expecting 1)", counter.decodeCount)
		}
		if counter.encodeCount != 1 {
			t.Fatalf("Unexpected encode count: %d (expecting 1)", counter.decodeCount)
		}

	}

}

type mapStruct struct {
	Key1 string
	Key2 string
}

func TestRawMap(t *testing.T) {
	var encoded []byte
	{
		m := map[string]string{
			"Key1": "Key1's value",
			"Key2": "Key2's value",
		}
		buf := bytes.NewBuffer(nil)
		encoder := NewEncoder(buf)
		if err := encoder.Encode(&m); err != nil {
			t.Fatalf("Error encoding counter: %s", err)
		}
		encoded = buf.Bytes()
	}

	{
		var empty interface{}
		decoder := NewDecoder(bytes.NewReader(encoded))
		decoder.DecodeMapFunc = DecodeMapToRaw
		if err := decoder.Decode(&empty); err != nil {
			t.Fatalf("Error decoding counter: %s\n\t%x", err, encoded)
		}
		raw, ok := empty.(RawMessage)
		if !ok {
			t.Fatalf("Unexpected decode type: %T", empty)
		}
		var s mapStruct
		if err := raw.Decode(&s); err != nil {
			t.Fatalf("Error decoding raw message: %s", err)
		}

		if expected := "Key1's value"; s.Key1 != expected {
			t.Fatalf("Unexpected value for Key1:\n\tExpected: %s\n\tActual: %s", expected, s.Key1)
		}

		if expected := "Key2's value"; s.Key2 != expected {
			t.Fatalf("Unexpected value for Key2:\n\tExpected: %s\n\tActual: %s", expected, s.Key2)
		}
	}
}
