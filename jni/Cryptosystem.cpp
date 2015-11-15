/*
 * Cryptosystem.cpp
 *
 *  Created on: Feb 4, 2015
 *      Author: kasdeya
 */

#include "Cryptosystem.h"

#ifndef MR_NOFULLWIDTH
Miracl precision(50,0);
#else
Miracl precision(50,MAXBASE);
#endif

/* elliptic curve prime */
const char* ecp[] = {
	"fffffffffffffffffffffffffffffffeffffffffffffffff", // p192
	"ffffffffffffffffffffffffffffffff000000000000000000000001", //p224
	"ffffffff00000001000000000000000000000000ffffffffffffffffffffffff",//p256
	"fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffeffffffff0000000000000000ffffffff",//p384
	"000001FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"//p521
	};

/*Group order */
const char* ecq[] = {
	"ffffffffffffffffffffffff99def836146bc9b1b4d22831", // p192
	"ffffffffffffffffffffffffffff16a2e0b8f03e13dd29455c5c2a3d", //p224
	"ffffffff00000000ffffffffffffffffbce6faada7179e84f3b9cac2fc632551",//p256
	"ffffffffffffffffffffffffffffffffffffffffffffffffc7634d81f4372ddf581a0db248b0a77aecec196accc52973",//p384
	"000001fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffa51868783bf2f966b7fcc0148f709a5d03bb5c9b8899c47aebb6fb71e91386409"//p521
	};

/* elliptic curve parameter B */
const char* ecb[] = {
	"64210519e59c80e70fa7e9ab72243049feb8deecc146b9b1",//p192
	"b4050a850c04b3abf54132565044b0b7d7bfd8ba270b39432355ffb4",//p224
	"5ac635d8aa3a93e7b3ebbd55769886bc651d06b0cc53b0f63bce3c3e27d2604b",//p256
	"b3312fa7e23ee7e4988e056be3f82d19181d9c6efe8141120314088f5013875ac656398d8a2ed19d2a85c8edd3ec2aef",//p384
	"00000051953eb9618e1c9a1f929a21a0b68540eea2da725b99b315f3b8b489918ef109e156193951ec7e937b1652c0bd3bb1bf073573df883d2c34f1ef451fd46b503f00"//p521
	};

/* elliptic curve - point of prime order (x,y) */
const char* ecx[] = {
	"188da80eb03090f67cbf20eb43a18800f4ff0afd82ff1012",//p192
	"b70e0cbd6bb4bf7f321390b94a03c1d356c21122343280d6115c1d21",//p244
	"6b17d1f2e12c4247f8bce6e563a440f277037d812deb33a0f4a13945d898c296",//p256
	"aa87ca22be8b05378eb1c71ef320ad746e1d3b628ba79b9859f741e082542a385502f25dbf55296c3a545e3872760aB7",//p384
	"000000c6858e06b70404e9cd9e3ecb662395b4429c648139053fb521f828af606b4d3dbaa14b5e77efe75928fe1dc127a2ffa8de3348b3c1856a429bf97e7e31c2e5bd66"//p521
	};

const char* ecy[] = {
	"07192b95ffc8da78631011ed6b24cdd573f977a11e794811",//p192
	"bd376388b5f723fb4c22dfe6cd4375a05a07476444d5819985007e34",//p224
	"4fe342e2fe1a7f9b8ee7eb4a7c0f9e162bce33576b315ececbb6406837bf51f5",//p256
	"3617de4a96262c6f5d9e98bf9292dc29f8f41dbd289a147ce9da3113b5f0b8c00a60b1ce1d7e819d7a431d7c90ea0e5F",//p384
	"0000011839296a789a3bc0045c8a5fb42c7d1bd998f54449579b446817afbd17273e662c97ee72995ef42640c550b9013fad0761353c7086a272c24088be94769fd16650"//p521
	};

static const char* const lut = "0123456789ABCDEF";

Cryptosystem* Cryptosystem::instance = new Cryptosystem();

Cryptosystem::Cryptosystem()
{
	mip = &precision;
	keyTokenizer = new EfficientTokenizer( (char *)";-" );
	decommitmentTokenizer = new EfficientTokenizer( ',' );
	isFirstDecommitment = true;
}

Cryptosystem::~Cryptosystem()
{
	delete keyTokenizer;
	delete decommitmentTokenizer;
	mip = NULL;
}

void Cryptosystem::initializeCommitmentBundle( char* key , int keyLength )
{
	int curve;
	s1.clear();
	s2.clear();
	keyTokenizer->setBuffer( key , keyLength );
	curve = atoi( keyTokenizer->getNthTokenPtr( 1 ) );
	keyTokenizer->revertEndPtrVal();
	keyTokenizer->resetBuffer();
	mip->IOBASE = 16;
	//read curve param
	a = -3;
	b = (char *)(ecb[curve]);
	p = (char *)(ecp[curve]);
	ecurve( a , b , p , MR_BEST );  // means use PROJECTIVE if possible, else AFFINE coordinates
	mip->IOBASE = 64;
}

void Cryptosystem::addToCommitmentBundle( char* commitment , int commitmentLength )
{
	int iy;
	ECn c1,c2;
	keyTokenizer->setBuffer( commitment , commitmentLength );
	x = keyTokenizer->getNthTokenPtr( 1 );
	keyTokenizer->revertEndPtrVal();
	iy = atoi( keyTokenizer->getNthTokenPtr( 2 ) );
	keyTokenizer->revertEndPtrVal();
	c1 = ECn( x , iy );
	x = keyTokenizer->getNthTokenPtr( 3 );
	keyTokenizer->revertEndPtrVal();
	iy = atoi( keyTokenizer->getNthTokenPtr( 4 ) );
	keyTokenizer->revertEndPtrVal();
	c2 = ECn( x , iy );
	if( s1.iszero() && s2.iszero() )
	{
		s1 = c1;
		s2 = c2;
	}
	else
	{
		s1 += c1;
		s2 += c2;
	}
	keyTokenizer->resetBuffer();
}

char* Cryptosystem::finalizeCommitmentBundle( void )
{
	int sy;
	char c[100];
	int sumSize = 206;
	char* sum;
	sum = new char[sumSize * sizeof( char )];
	memset( sum , '\0' , sumSize );
	//write com
	sy = ( s1.get(x) == 1 ? 1 : 0 );
	memset( c , '\0' , 100 );
	c << x;
	sprintf( sum , "%s-%d",c,sy);
	sy = ( s2.get(x) == 1 ? 1 : 0 );
	c << x;
	sprintf( sum + strlen( sum ), ";%s-%d",c,sy);
	return sum;
}

void Cryptosystem::initializeDecommitmentBundle( char* key , int keyLength )
{
	isFirstDecommitment = true;
	//parse key string
	//only need q
	keyTokenizer->setBuffer( key , keyLength );
	int curve = atoi( keyTokenizer->getNthTokenPtr( 1 ) );
	keyTokenizer->revertEndPtrVal();
	mip->IOBASE=16;
	q=(char *)(ecq[curve]);
	mip->IOBASE=64;
	keyTokenizer->resetBuffer();
}

void Cryptosystem::addToDecommitmentBundle( char* decommitment , int decommitmentLength )
{
	decommitmentTokenizer->setBuffer( decommitment , decommitmentLength );
	x = decommitmentTokenizer->getNthTokenPtr( 1 );
	decommitmentTokenizer->revertEndPtrVal();
	Big y = decommitmentTokenizer->getNthTokenPtr( 2 );
	if( isFirstDecommitment )
	{
		isFirstDecommitment = false;
		bs1 = x;
		bs2 = y;
	}
	else
	{
		bs1 += x;
		bs2 += y;
	}
	decommitmentTokenizer->resetBuffer();
}

char* Cryptosystem::finalizeDecommitmentBundle( void )
{
	int sumSize = 202;
	char* sum;
	char c[100];
	sum = new char[sumSize * sizeof( char )];
	memset( sum , '\0' , sumSize );
	x = 1;
	bs1 = modmult( bs1 , x , q );
	bs2 = modmult( bs2 , x , q );
	mip->IOBASE = 64;
	c << bs1;
	sprintf( sum ,"%s,",c);
	c << bs2;
	strcat( sum , c );
	return sum;

}

bool Cryptosystem::verifyCommitments( char* commitmentBundle , int commitmentBundleLength ,
		char* decommitmentBundle , int decommitmentBundleLength ,
		char* key , int keyLength )
{
	Big d1,d2,y;
	ECn c1,c2,g,h,tempE;
	//parse key string
	keyTokenizer->setBuffer( key , keyLength );
	int curve = atoi( keyTokenizer->getNthTokenPtr( 1 ) );
	keyTokenizer->revertEndPtrVal();
	//Read g
	a = -3;
	mip->IOBASE=16;
	b = (char *)(ecb[curve]);
	p = (char *)(ecp[curve]);
	ecurve( a , b , p , MR_BEST );  // means use PROJECTIVE if possible, else AFFINE coordinates
	x = (char *)(ecx[curve]);
	y = (char *)(ecy[curve]);
	g = ECn( x , y );
	//Read PK
	mip->IOBASE=64;
	x = keyTokenizer->getNthTokenPtr( 2 );
	keyTokenizer->revertEndPtrVal();
	int iy = atoi( keyTokenizer->getNthTokenPtr( 3 ) );
	keyTokenizer->revertEndPtrVal();
	h = ECn( x , iy );
	keyTokenizer->resetBuffer();
	//parse commitment
	keyTokenizer->setBuffer( commitmentBundle , commitmentBundleLength );
	x = keyTokenizer->getNthTokenPtr( 1 );
	keyTokenizer->revertEndPtrVal();
	iy = atoi( keyTokenizer->getNthTokenPtr( 2 ) );
	keyTokenizer->revertEndPtrVal();
	c1 = ECn( x , iy );
	x = keyTokenizer->getNthTokenPtr( 3 );
	keyTokenizer->revertEndPtrVal();
	iy = atoi( keyTokenizer->getNthTokenPtr( 4 ) );
	keyTokenizer->revertEndPtrVal();
	c2 = ECn( x , iy );
	keyTokenizer->resetBuffer();
	//parse decommitments only need first half
	decommitmentTokenizer->setBuffer( decommitmentBundle , decommitmentBundleLength );
	d1 = decommitmentTokenizer->getNthTokenPtr( 1 );
	decommitmentTokenizer->revertEndPtrVal();
	d2 = decommitmentTokenizer->getNthTokenPtr( 2 );
	decommitmentTokenizer->revertEndPtrVal();
	decommitmentTokenizer->resetBuffer();
	//verify
	s1 = d2 * g;
	s2 = d1 * g;
	tempE = d2 * h;
	s2 += tempE;
	if( c1 == s1 && c2 == s2 )
		return true;
	return false;
}

char* Cryptosystem::tally( char* decommitmentBundle , int decommitmentBundleLength , int N , int m )
{
	int tallySize = ( m * 100 ) + 2;
	Big y,tempB,Num;
	Num = N + 1;
	char c[100];
	char* tally;
	tally = new char[tallySize * sizeof(char)];
	memset( tally , '\0' , tallySize * sizeof( char ) );
	decommitmentTokenizer->setBuffer( decommitmentBundle , decommitmentBundleLength );
	x = decommitmentTokenizer->getNthTokenPtr( 1 );
	decommitmentTokenizer->revertEndPtrVal();
	decommitmentTokenizer->resetBuffer();
	mip->IOBASE = 10;
	for( int i = m - 1 ; i >= 0 ; --i )
	{
		y = x/Num;
		tempB = y*Num;
		tempB = x - tempB;
		c << tempB;
		if( i == m - 1 )
			strcpy( tally , c );
		else
			sprintf( tally + strlen( tally ) ,",%s",c);
		x = y;

	}
	return tally;
}


void Cryptosystem::hex2string(const std::string& input, std::string& output)
{
    size_t len = input.length();
    if (len & 1) throw std::invalid_argument("odd length");

    output.clear();
    output.reserve(len / 2);
    for (size_t i = 0; i < len; i += 2)
    {
        char a = input[i];
        const char* p = lower_bound(lut, lut + 16, a);
        if (*p != a) throw std::invalid_argument("not a hex digit for input 1");

        char b = input[i + 1];
        const char* q = lower_bound(lut, lut + 16, b);
        if (*q != b) throw std::invalid_argument("not a hex digit for input 2");

        output.push_back(((p - lut) << 4) | (q - lut));
    }
}
