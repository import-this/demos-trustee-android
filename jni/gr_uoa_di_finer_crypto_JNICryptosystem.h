/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class gr_uoa_di_finer_crypto_JNICryptosystem */

#ifndef _Included_gr_uoa_di_finer_crypto_JNICryptosystem
#define _Included_gr_uoa_di_finer_crypto_JNICryptosystem
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     gr_uoa_di_finer_crypto_JNICryptosystem
 * Method:    initializeCommitmentBundle
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_gr_uoa_di_finer_crypto_JNICryptosystem_initializeCommitmentBundle
  (JNIEnv *, jobject, jstring);

/*
 * Class:     gr_uoa_di_finer_crypto_JNICryptosystem
 * Method:    addToCommitmentBundle
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_gr_uoa_di_finer_crypto_JNICryptosystem_addToCommitmentBundle
  (JNIEnv *, jobject, jstring);

/*
 * Class:     gr_uoa_di_finer_crypto_JNICryptosystem
 * Method:    finalizeCommitmentBundle
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_gr_uoa_di_finer_crypto_JNICryptosystem_finalizeCommitmentBundle
  (JNIEnv *, jobject);

/*
 * Class:     gr_uoa_di_finer_crypto_JNICryptosystem
 * Method:    initializeDecommitmentBundle
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_gr_uoa_di_finer_crypto_JNICryptosystem_initializeDecommitmentBundle
  (JNIEnv *, jobject, jstring);

/*
 * Class:     gr_uoa_di_finer_crypto_JNICryptosystem
 * Method:    addToDecommitmentBundle
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_gr_uoa_di_finer_crypto_JNICryptosystem_addToDecommitmentBundle
  (JNIEnv *, jobject, jstring);

/*
 * Class:     gr_uoa_di_finer_crypto_JNICryptosystem
 * Method:    finalizeDecommitmentBundle
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_gr_uoa_di_finer_crypto_JNICryptosystem_finalizeDecommitmentBundle
  (JNIEnv *, jobject);

/*
 * Class:     gr_uoa_di_finer_crypto_JNICryptosystem
 * Method:    verifyCommitments
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_gr_uoa_di_finer_crypto_JNICryptosystem_verifyCommitments
  (JNIEnv *, jobject, jstring, jstring, jstring);

/*
 * Class:     gr_uoa_di_finer_crypto_JNICryptosystem
 * Method:    tally
 * Signature: (Ljava/lang/String;II)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_gr_uoa_di_finer_crypto_JNICryptosystem_tally
  (JNIEnv *, jobject, jstring, jint, jint);

#ifdef __cplusplus
}
#endif
#endif
