package de.tubs.cs.iti.krypto.protokoll.oblivious;

import java.math.BigInteger;

public class SecretSharingProtocolData {

  /** The number of different letters used in the alphabet. */
  public final int BASE; //= 16; // 36

  /** The maximal number of letters used in each word. */
  public final int LETTERS; // = 4; // 10

  /**
   * The maximal number of different messages depending on BASE and LETTER:
   * MAX_MESSAGE_NUMBER = BASE^LETTERS.
   */
  public final BigInteger MAX_MESSAGE_NUMBER;

  /**
   * The number of bits used in each word depending on BASE and LETTERS: <br />
   * Math.floor(LETTERS * Math.log(BASE) / Math.log(2)) <br />
   * Example: BASE=36 and LETTERS=10 => BITS=52.
   */
  public final int BITS; // = (int) Math.floor(LETTERS * Math.log(BASE) / Math.log(2));

  /** The advantage over the other attendant. 2^k+1 over 2^k; max. 7. */
  public final int k; // = 3; // 7

  /**
   * Class stores data for the secret sharing protocol.
   * @param base The number of letters used in the alphabet.
   * @param letters The number of letters used in each word.
   * @param k The advantage over the other attendant. 2^k+1 over 2^k; max. 7.
   */
  public SecretSharingProtocolData(int base, int letters, int k) {
    this.BASE = base;
    this.LETTERS = letters;
    this.k = k;
    MAX_MESSAGE_NUMBER = BigInteger.valueOf(BASE).pow(LETTERS);
    BITS = (int) Math.floor(LETTERS * Math.log(BASE) / Math.log(2));
  }

  /**
   * @return BASE=36, LETTERS=10, k=7.
   */
  public static SecretSharingProtocolData getDefaultData() {
    return new SecretSharingProtocolData(36, 10, 7);
  }

  /**
   * @return BASE=16, LETTERS=4, k=3.
   */
  public static SecretSharingProtocolData getTestData() {
    return new SecretSharingProtocolData(16, 4, 3);
  }
}
