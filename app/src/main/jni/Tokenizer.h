#ifndef __TOKENIZER_H__
#define __TOKENIZER_H__

#include <cstdlib>
#include <cstring>
#include <iostream>
#include <stdexcept>
#include <exception>

using namespace std;

class Tokenizer
{
	public:
		Tokenizer()
		{
			numOfDelimiters = 1;
			delimiterArray = (char *)malloc( sizeof( char ) );
			if( delimiterArray == NULL )
				throw runtime_error( (char *)"Tokenizer::Tokenizer() : Failed to allocate memory for the delimiter array...!");
			delimiterArray[0] = ' ';
			bufferPtr = NULL;
			bufferSize = 0;
			tokenEndPtr = NULL;
			endCharVal = '\0';
			numOfTokens = -1;
		}
		Tokenizer( char delim )
		{
			numOfDelimiters = 1;
			delimiterArray = (char *)malloc( sizeof( char ) );
			if( delimiterArray == NULL )
				throw runtime_error( (char *)"Tokenizer::Tokenizer() : Failed to allocate memory for the delimiter array...!");
			delimiterArray[0] = delim;
			bufferPtr = NULL;
			bufferSize = 0;
			tokenEndPtr = NULL;
			endCharVal = '\0';
			numOfTokens = -1;
		}
		Tokenizer( char* delims )
		{
			if( delims == NULL )
				throw runtime_error( (char *)"Tokenizer::Tokenizer() : Why did you call this constructor with a NULL pointer as argument?" );
			numOfDelimiters = strlen( delims );
			delimiterArray = (char *)malloc( numOfDelimiters * sizeof( char ) );
			for( int i = 0 ; i < numOfDelimiters ; ++i )
				delimiterArray[i] = delims[i];
			bufferPtr = NULL;
			bufferSize = 0;
			tokenEndPtr = NULL;
			endCharVal = '\0';
			numOfTokens = -1;
		}
		virtual ~Tokenizer()
		{
			if( numOfDelimiters > 0 )
			{
				numOfDelimiters = 0;
				free( delimiterArray );
				delimiterArray = NULL;
			}
		}
		virtual void setBuffer( char* buffer , int size ) = 0;
		virtual void resetBuffer( void ) = 0;
		virtual char* getNthTokenPtr( int n ) = 0;
		virtual void saveEndPtrVal( void );
		virtual void revertEndPtrVal( void );
		virtual void addDelimiter( char c );
		virtual void removeDelimiter( char c );
		virtual void clearDelimiters( void );
		virtual bool isDelimiter( char c );
		virtual int getNumOfTokens( void );
		virtual inline int getNumOfDelimiters( void )
		{
			return numOfDelimiters;
		}
	protected:
		virtual char* goAtFirstNthDelimiter( int startingOffset , int n );
		virtual char* goAtLastNthDelimiter( int startingOffset , int n );
		virtual char* goAfterFirstNthDelimiter( int startingOffset , int n );
		virtual char* goAfterLastNthDelimiter( int startingOffset , int n );
		virtual void goAtEndOfDelimiterTrail( int* curIndex );
		virtual void moveIfRequiredPastDelimiter( int* curIndex );
		char* bufferPtr;
		int bufferSize;
		char* tokenEndPtr;
		char endCharVal;
		int numOfTokens;
	private:
		int numOfDelimiters;
		char* delimiterArray;
};

class EfficientTokenizer : public Tokenizer
{
	public:
		EfficientTokenizer()
		{
			tokenStartPtr = NULL;
			currentTokenNum = 0;
		}
		EfficientTokenizer( char delim ) : Tokenizer( delim )
		{
			tokenStartPtr = NULL;
			currentTokenNum = 0;
		}
		EfficientTokenizer( char* delims ) : Tokenizer( delims )
		{
			tokenStartPtr = NULL;
			currentTokenNum = 0;
		}
		virtual ~EfficientTokenizer()
		{
		}
		virtual void setBuffer( char* buffer , int size )
		{
			if( size < 0 )
				throw runtime_error( (char *)"EfficientTokenizer::setBuffer() : Invalid specified buffer size...!");
			if( buffer == NULL )
				throw runtime_error( (char *)"EfficientTokenizer::setBuffer() : Buffer parameter was found to be NULL...!");
			revertEndPtrVal();
			tokenEndPtr = NULL;
			endCharVal = '\0';
			bufferSize = size;
			bufferPtr = buffer;
			tokenStartPtr = NULL;
			currentTokenNum = 0;
			numOfTokens = -1;
		}
		virtual void resetBuffer( void )
		{
			tokenEndPtr = NULL;
			endCharVal = '\0';
			bufferSize = 0;
			bufferPtr = NULL;
			tokenStartPtr = NULL;
			currentTokenNum = 0;
			numOfTokens = -1;			
		}
		virtual char* getNthTokenPtr( int n );
	private:
		char* tokenStartPtr;
		int currentTokenNum;
};

class SafeTokenizer : public Tokenizer
{
	public:
		SafeTokenizer()
		{
		}
		virtual ~SafeTokenizer()
		{
			if( bufferPtr != NULL )
			{
				delete [] bufferPtr;
				bufferSize = 0;
			}
		}
		virtual void setBuffer( char* buffer , int size )
		{
			if( size < 0 )
				throw runtime_error( (char *)"SafeTokenizer:			revertEndPtrVal();:setBuffer() : Invalid specified buffer size...!");
			if( buffer == NULL )
				throw runtime_error( (char *)"SafeTokenizer::setBuffer() : Buffer parameter was found to be NULL...!");
			if( bufferPtr != NULL )
				delete [] bufferPtr;
			tokenEndPtr = NULL;
			bufferSize = size;
			bufferPtr = new char[bufferSize+1];
			memcpy( bufferPtr , buffer , bufferSize * sizeof( char ) );
			bufferPtr[bufferSize] = '\0';
		}
		virtual void resetBuffer( void )
		{
		}
		virtual char* getNthTokenPtr( int n );
	private:
};
#endif
