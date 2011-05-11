package task3;

public class UInt16 {

  private int value;
  
  public UInt16(byte[] a, int pos) {
    int high = a[pos]   & 0xFF;
    int low  = a[pos+1] & 0xFF;
    value = (high << 8) + low;
  }
  
  public UInt16(UInt16 uint16) {
    value = uint16.value;
  }

  public int getValue() {
    return value;
  }
  
  public void xor(UInt16 uint16) {
    value = (value ^ uint16.value) & 0xFFFF;
  }

  public void add(UInt16 uint16) {
    value = (value + uint16.value) & 0xFFFF;
  }
  /**
   * Multiplication modulo 2^16+1=65537 used for IDEA.
   * If input  is 0    it is replaced by 2^16.
   * If output is 2^16 it is replaced by 0.
   * @param uint16 The second factor for multiplication.
   */
  public void mul(UInt16 uint16) {
    long uint32 = (long) value * uint16.value;
    if (uint32==0) { // this.value==0 and/or uint16.value==0 
      value = (65537-value-uint16.value) & 0xFFFF;
    } else {
      value        = (int)(uint32 >> 16);
      uint16.value = (int)(uint32 & 0xFFFF);
      value        = (uint16.value-value) & 0xFFFF;
      if (uint16.value < value) {
        value = (value + 65537) & 0xFFFF;
      }
    }

  }

  public void copyTo(byte[] out, int pos) {
    out[pos]   = (byte) ((value >> 8) & 0xFF);
    out[pos+1] = (byte) ( value       & 0xFF);
  }

  /**
   * Value of this UInt16 as a hex-string.
   */
  @Override
  public String toString() {
    return Integer.toString(value, 16);
  }

}
