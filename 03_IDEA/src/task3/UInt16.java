package task3;

import java.math.BigInteger;

class IntTriple {
  int p1;
  int p2;
  int p3;

  IntTriple(int p1_, int p2_, int p3_){
    p1 = p1_;
    p2 = p2_;
    p3 = p3_;
  }
}

public class UInt16 {

  private static final int twoPow16 = 1 << 16;
  private int value;

  public UInt16(byte[] a, int pos) {
    int high = (char)a[pos]   & 0xFF;
    int low  = (char)a[pos+1] & 0xFF;
    value = (high << 8) + low;
  }

  public UInt16(UInt16 uint16) {
    value = uint16.value;
  }

  /**
   * Get lower 16 bit of a.
   * @param a
   */
  public UInt16(BigInteger a) {
    int a_ = a.intValue();
    value = a_ & 0xFFFF;
  }

  public UInt16(long a) {
    assert a <= 0xFFFF;
    value = (int)a;
  }

  public int getValue() {
    return value;
  }

  public void xor(UInt16 uint16) {
    value = value ^ uint16.value;
  }

  public void add(UInt16 uint16) {
    long expected = (value + uint16.value) % twoPow16;
    value = (value + uint16.value) & 0xFFFF;
    assert value == expected : "got=" + value + ", expected=" + expected;
  }

  /**
   * Multiplication modulo 2^16+1=65537 used for IDEA.
   * If input  is 0    it is replaced by 2^16.
   * If output is 2^16 it is replaced by 0.
   * @param uint16 The second factor for multiplication.
   */
  public void mul(UInt16 uint16) {
    // save values for postcondition
    int oldValThis = this.value;
    int oldValOther = uint16.value;

    long uint32 = (long) value * uint16.value;
    if (uint32==0) { // this.value==0 and/or uint16.value==0
      value = (65537-value-uint16.value) & 0xFFFF;
    } else {
      value        = (int)(uint32 >> 16);
      int rhsVal = (int)(uint32 & 0xFFFF);
      value        = (rhsVal - value) & 0xFFFF;
      if (rhsVal < value) {
        value = (value + 65537) & 0xFFFF;
      }
    }

    // postcondition
    {
      long lhs = (oldValThis == 0) ? twoPow16 : oldValThis;
      long rhs = (oldValOther == 0) ? twoPow16 : oldValOther;
      long res = (lhs * rhs) % (twoPow16 + 1);
      res = (res == twoPow16) ? 0 : res;
      assert this.value == res : "in " + lhs + " * " + rhs + " got=" + this.value + ", expected=" + res;
      assert uint16.value == oldValOther;
    }
  }

  public void copyTo(byte[] out, int pos) {
    out[pos]   = (byte) ((value >> 8) & 0xFF);
    out[pos+1] = (byte) ( value       & 0xFF);
  }

  public void copyTo(char[] out, int pos) {
    out[pos]   = (char) ((value >> 8) & 0xFF);
    out[pos+1] = (char) ( value       & 0xFF);
  }

  /**
   * Value of this UInt16 as a hex-string.
   */
  @Override
  public String toString() {
    //return Integer.toString(value, 16);
    return Integer.toString(value, 10);
  }

  public UInt16 invert() {
    int inv = modinv(value, twoPow16 + 1);
    return new UInt16(inv);
  }

  public UInt16 negate() {
    long expected = (twoPow16 - value) % twoPow16;
    long res = (twoPow16 - value) & 0xFFFF;
    assert res == expected : "got=" + value + ", expected=" + expected;
    return new UInt16(res);
  }

  /**
   * Erweiterter Euklidscher Alg.
   * 
   * @param x
   * @param y
   * @return
   */
  private static IntTriple gcdExt(int x, int y){
    assert x < y;
    assert x >= 0;
    assert y > 0;

    if(x == 0){
      return new IntTriple(0, 1, y);
    }
    else{
      int d = y / x;
      int r = y - d * x;
      IntTriple abg = gcdExt(r, x);

      return new IntTriple(abg.p2 - abg.p1 * d, abg.p1, abg.p3);
    }
  }

  /**
   * Berechnet das modulare Inverse von x mod n.
   * 
   * @param x
   * @param n
   * @return
   */
  private static int modinv(int x, int n){
    IntTriple abg = gcdExt(x % n, n);
    assert abg.p3 == 1;

    int r = abg.p1;
    if(r < 0){
      r = r + ((Math.abs(r) / n) + 1) * n;
    }

    return r % n;
  }

  public void swap(UInt16 a) {
    int t = this.value;
    this.value = a.value;
    a.value = t;

  }

}
