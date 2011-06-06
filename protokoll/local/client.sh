#!/bin/sh
if [ -z $KRYPTOUMGEBUNG ]; then
  echo Warnung: Variable KRYPTOUMGEBUNG nicht gesetzt! 
  echo Starte trotzdem.
  java de.tubs.cs.iti.krypto.protokoll.Client $*
else
  java -classpath "$KRYPTOUMGEBUNG/03_IDEA/bin/task3:$KRYPTOUMGEBUNG/04_ElGamal/bin/task4:$KRYPTOUMGEBUNG/05_Fingerprint/bin/task5:$KRYPTOUMGEBUNG/protokoll/classes.jar:." de.tubs.cs.iti.krypto.protokoll.Client $*
fi
