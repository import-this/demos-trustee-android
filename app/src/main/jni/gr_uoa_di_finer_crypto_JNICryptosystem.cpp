/*
 * JNICryptosystem.cpp
 *
 *  Created on: Feb 6, 2015
 *      Author: kasdeya
 */

#include "Cryptosystem.h"
#include "gr_uoa_di_finer_crypto_JNICryptosystem.h"

/*
 * Class:     gr_uoa_di_finer_crypto_JNICryptosystem
 * Method:    initializeCommitmentBundle
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_gr_uoa_di_finer_crypto_JNICryptosystem_initializeCommitmentBundle
  (JNIEnv *env, jobject thisObject, jstring JNIkey)
{
	jboolean isCopy;
	const char* key = env->GetStringUTFChars( JNIkey , &isCopy );
	int keyLength = env->GetStringUTFLength( JNIkey );
	string encodedKey( key , keyLength );
	string decodedKey;
	Cryptosystem::getInstance()->hex2string( encodedKey , decodedKey );
	Cryptosystem::getInstance()->initializeCommitmentBundle( (char *)decodedKey.c_str() , decodedKey.size() );
	if( isCopy )
		env->ReleaseStringUTFChars( JNIkey , key );
}

/*
 * Class:     gr_uoa_di_finer_crypto_JNICryptosystem
 * Method:    addToCommitmentBundle
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_gr_uoa_di_finer_crypto_JNICryptosystem_addToCommitmentBundle
  (JNIEnv *env, jobject thisObject, jstring JNIcommitment)
{
	jboolean isCopy;
	const char* commitment = env->GetStringUTFChars( JNIcommitment , &isCopy );
	int commitmentLength = env->GetStringUTFLength( JNIcommitment );
	Cryptosystem::getInstance()->addToCommitmentBundle( (char *)commitment , commitmentLength );
	if( isCopy )
		env->ReleaseStringUTFChars( JNIcommitment , commitment );
}

/*
 * Class:     gr_uoa_di_finer_crypto_JNICryptosystem
 * Method:    finalizeCommitmentBundle
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_gr_uoa_di_finer_crypto_JNICryptosystem_finalizeCommitmentBundle
  (JNIEnv *env, jobject thisObject)
{
	char* commitmentBundle = Cryptosystem::getInstance()->finalizeCommitmentBundle();
	jstring JNIcommitmentBundle = env->NewStringUTF( commitmentBundle );
	delete [] commitmentBundle;
	return JNIcommitmentBundle;
}

/*
 * Class:     gr_uoa_di_finer_crypto_JNICryptosystem
 * Method:    initializeDecommitmentBundle
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_gr_uoa_di_finer_crypto_JNICryptosystem_initializeDecommitmentBundle
  (JNIEnv *env, jobject thisObject, jstring JNIkey)
{
	jboolean isCopy;
	const char* key = env->GetStringUTFChars( JNIkey , &isCopy );
	int keyLength = env->GetStringUTFLength( JNIkey );
	string encodedKey( key , keyLength );
	string decodedKey;
	Cryptosystem::getInstance()->hex2string( encodedKey , decodedKey );
	Cryptosystem::getInstance()->initializeDecommitmentBundle( (char *)decodedKey.c_str() , decodedKey.size() );
	if( isCopy )
		env->ReleaseStringUTFChars( JNIkey , key );
}

/*
 * Class:     gr_uoa_di_finer_crypto_JNICryptosystem
 * Method:    addToDecommitmentBundle
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_gr_uoa_di_finer_crypto_JNICryptosystem_addToDecommitmentBundle
  (JNIEnv *env, jobject thisObject, jstring JNIdecommitment)
{
	jboolean isCopy;
	const char* decommitment = env->GetStringUTFChars( JNIdecommitment , &isCopy );
	int decommitmentLength = env->GetStringUTFLength( JNIdecommitment );
	Cryptosystem::getInstance()->addToDecommitmentBundle( (char *)decommitment , decommitmentLength );
	if( isCopy )
		env->ReleaseStringUTFChars( JNIdecommitment , (char *)decommitment );
}

/*
 * Class:     gr_uoa_di_finer_crypto_JNICryptosystem
 * Method:    finalizeDecommitmentBundle
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_gr_uoa_di_finer_crypto_JNICryptosystem_finalizeDecommitmentBundle
  (JNIEnv *env, jobject thisObject)
{
	char* decommitmentBundle = Cryptosystem::getInstance()->finalizeDecommitmentBundle();
	jstring JNIdecommitmentBundle = env->NewStringUTF( decommitmentBundle );
	delete [] decommitmentBundle;
	return JNIdecommitmentBundle;
}

/*
 * Class:     gr_uoa_di_finer_crypto_JNICryptosystem
 * Method:    verifyCommitments
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_gr_uoa_di_finer_crypto_JNICryptosystem_verifyCommitments
  (JNIEnv *env, jobject thisObject, jstring JNIcommitmentBundle, jstring JNIdecommitmentBundle, jstring JNIkey)
{
	jboolean isCommitmentBundleCopy,isDecommitmentBundleCopy,isKeyCopy;
	const char* commitmentBundle = env->GetStringUTFChars( JNIcommitmentBundle , &isCommitmentBundleCopy );
	int commitmentBundleLength = env->GetStringUTFLength( JNIcommitmentBundle );
	const char* decommitmentBundle = env->GetStringUTFChars( JNIdecommitmentBundle , &isDecommitmentBundleCopy );
	int decommitmentBundleLength = env->GetStringUTFLength( JNIdecommitmentBundle );
	const char* key = env->GetStringUTFChars( JNIkey , &isKeyCopy );
	int keyLength = env->GetStringUTFLength( JNIkey );
	string encodedKey( key , keyLength );
	string decodedKey;
	Cryptosystem::getInstance()->hex2string( encodedKey , decodedKey );
	jboolean match = Cryptosystem::getInstance()->verifyCommitments( (char *)commitmentBundle , commitmentBundleLength ,
			(char *)decommitmentBundle , decommitmentBundleLength ,
			(char *)decodedKey.c_str() , decodedKey.length() );
	if( isCommitmentBundleCopy )
		env->ReleaseStringUTFChars( JNIcommitmentBundle , commitmentBundle );
	if( isDecommitmentBundleCopy )
		env->ReleaseStringUTFChars( JNIdecommitmentBundle , decommitmentBundle );
	if( isKeyCopy )
		env->ReleaseStringUTFChars( JNIkey , key );
	return match;
}

/*
 * Class:     gr_uoa_di_finer_crypto_JNICryptosystem
 * Method:    tally
 * Signature: (Ljava/lang/String;II)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_gr_uoa_di_finer_crypto_JNICryptosystem_tally
  (JNIEnv *env, jobject thisObject, jstring JNIdecommitmentBundle , jint N, jint m)
{
	jboolean isCopy;
	const char* decommitmentBundle = env->GetStringUTFChars( JNIdecommitmentBundle , &isCopy );
	int decommitmentBundleLength = env->GetStringUTFLength( JNIdecommitmentBundle );
	char* tallyResult = Cryptosystem::getInstance()->tally( (char *)decommitmentBundle , decommitmentBundleLength , N , m );
	if( isCopy )
		env->ReleaseStringUTFChars( JNIdecommitmentBundle , decommitmentBundle );
	jstring JNItallyResult = env->NewStringUTF( tallyResult );
	return JNItallyResult;
}
