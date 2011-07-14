package de.tubs.cs.iti.krypto.protokoll.util;

import java.math.BigInteger;
import java.util.Random;

import de.tubs.cs.iti.jcrypt.chiffre.BigIntegerUtil;

public class PohligHellman {

  public static BigInteger generateKey(Random rnd, BigInteger p){
    final BigInteger pMinus1 = p.subtract(BigInteger.ONE);
    BigInteger e;
    do{
      e = BigIntegerUtil.randomBetween(BigInteger.valueOf(2), pMinus1);
    } while(!e.gcd(pMinus1).equals(BigInteger.ONE));

    return e;
  }

  public static BigInteger getDecryptKey(BigInteger p, BigInteger e){
    return e.modInverse(p);
  }

  public static BigInteger encrypt(BigInteger p, BigInteger e, BigInteger M){
    assert M.compareTo(BigInteger.ZERO) > 0 && M.compareTo(p) < 0;
    return M.modPow(e, p);
  }

  public static BigInteger decrypt(BigInteger p, BigInteger e, BigInteger M){
    return encrypt(p, getDecryptKey(p, e), M);
  }
}
