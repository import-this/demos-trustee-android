/*
 * Cryptosystem.h
 *
 *  Created on: Feb 4, 2015
 *      Author: kasdeya
 */

#ifndef SRC_CRYPTOSYSTEM_H_
#define SRC_CRYPTOSYSTEM_H_

#include <string>
#include <stdlib.h>     /* atoi */
#include <iostream>
#include <fstream>
#include <sstream>
#include <math.h>       /* log2 */
#include "ecn.h"
#include "big.h"
#include "miracl.h" //sha-2
#include "Tokenizer.h"

using namespace std;

class Cryptosystem
{
public:
	static inline Cryptosystem* getInstance( void )
	{
		return instance;
	}
	static inline void deleteInstance( void )
	{
		delete instance;
	}
	~Cryptosystem();
	//Commitment functions
	void initializeCommitmentBundle( char* key , int keyLength );
	void addToCommitmentBundle( char* commitment , int commitmentLength );
	char* finalizeCommitmentBundle( void );
	//Decommitment functions
	void initializeDecommitmentBundle( char* key , int keyLength );
	void addToDecommitmentBundle( char* decommitment , int decommitmentBundleLength );
	char* finalizeDecommitmentBundle( void );
	//Verify that the commitment bundle matches the decommitment bundle given the right key
	bool verifyCommitments( char* commitmentBundle , int commitmentBundleLength ,
			char* decommitmentBundle , int decommitmentBundleLength ,
			char* key , int keyLength );
	//Tally the decommitment bundle
	char* tally( char* decommitmentBundle , int decommitmentBundleLength , int N , int m );
	//
	void hex2string(const string& input, string& output);
private:
	Cryptosystem();
	static Cryptosystem* instance;
	//Common state
	miracl *mip;
	Big x;
	EfficientTokenizer* keyTokenizer;
	//Commitment bundle state
	Big a;
	Big b;
	Big p;
	ECn s1;
	ECn s2;
	//Decommitment bundle state
	Big q;
	Big bs1;
	Big bs2;
	bool isFirstDecommitment;
	EfficientTokenizer* decommitmentTokenizer;
};

#endif /* SRC_CRYPTOSYSTEM_H_ */
