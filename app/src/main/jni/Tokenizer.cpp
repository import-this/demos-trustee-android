#include "Tokenizer.h"

void Tokenizer::saveEndPtrVal( void )
{
	endCharVal = *tokenEndPtr;
	*tokenEndPtr = '\0';
}

void Tokenizer::revertEndPtrVal( void )
{
	if( tokenEndPtr != NULL && bufferPtr != NULL )
	{
		*tokenEndPtr = endCharVal;
		tokenEndPtr = NULL;
		endCharVal = '\0';
	}
}

void Tokenizer::addDelimiter( char c )
{
	++numOfDelimiters;
	char *tempArrayPtr;
	tempArrayPtr = (char *)realloc( delimiterArray , numOfDelimiters * sizeof( char ) );
	if( tempArrayPtr == NULL )
		throw runtime_error( (char *)"Tokenizer::addDelimiter() : Delimiter array reallocation failed...!");
	delimiterArray = tempArrayPtr;
	delimiterArray[numOfDelimiters-1] = c;
}

void Tokenizer::removeDelimiter( char c )
{
	for( int i = 0 ; i < numOfDelimiters ; ++i )
	{
		if( delimiterArray[i] == c )
		{
			if( i < numOfDelimiters - 1 )
				delimiterArray[i] = delimiterArray[numOfDelimiters-1];
			--numOfDelimiters;
			char *tempArrayPtr;
			tempArrayPtr = (char *)realloc( delimiterArray , numOfDelimiters * sizeof( char ) );
			if( tempArrayPtr == NULL )
				throw runtime_error( (char *)"Tokenizer::addDelimiter() : Delimiter array reallocation failed...!");
			delimiterArray = tempArrayPtr;
		}
	}
}

void Tokenizer::clearDelimiters( void )
{
	if( numOfDelimiters == 0 )
		return;
	free( delimiterArray );
	delimiterArray = NULL;
	numOfDelimiters = 0;
}

bool Tokenizer::isDelimiter( char c )
{
	for( int i = 0 ; i < numOfDelimiters ; ++i )
	{
		if( delimiterArray[i] == c )
			return true;
	}
	return false;
}

int Tokenizer::getNumOfTokens( void )
{
	int i = 0;
	if( bufferPtr == NULL )
		throw runtime_error( (char *)"Tokenizer::getNumOfTokens() : My buffer is NULL, how am I supposed to search for tokens...?");
	if( numOfTokens == -1 )
	{
		numOfTokens = 0;
		goAtEndOfDelimiterTrail( &i );
		moveIfRequiredPastDelimiter( &i );
		if( !isDelimiter( bufferPtr[i] ) )
			++numOfTokens;
		else if( i == bufferSize - 1 )
			return numOfTokens;
		for( ; i < bufferSize ; ++i )
		{
			if( isDelimiter( bufferPtr[i] ) )
			{
				goAtEndOfDelimiterTrail( &i );
				moveIfRequiredPastDelimiter( &i );
				if( !isDelimiter( bufferPtr[i] ) )
					++numOfTokens;
			}
		}
	}
	return numOfTokens;
}

char* Tokenizer::goAtFirstNthDelimiter( int startingOffset , int n )
{
	int i = startingOffset;
	if( bufferPtr == NULL )
		throw runtime_error( (char *)"Tokenizer::goAtFirstNthDelimiter() : My buffer is NULL, how am I supposed to search for delimiters...?");
	if( startingOffset < 0 || startingOffset > bufferSize )
		throw runtime_error( (char *)"Tokenizer::goAtFirstNthDelimiter() : Specified starting offset is invalid (either negative or bigger than the size of the buffer...?");
	if( n < 0 )
		throw runtime_error( (char *)"Tokenizer::goAtFirstNthDelimiter() : Delimiter number needs to be a positive integer...?");
	else if( n == 0 )
		return (bufferPtr+i);
	//skipping all trailing delimiters;
	goAtEndOfDelimiterTrail( &i );
	moveIfRequiredPastDelimiter( &i );
	char *delimPtr = NULL;
	int occuredDelimiters = 0;
	for( ; i < bufferSize ; ++i )
	{
		if( isDelimiter( bufferPtr[i] ) )
		{
			++occuredDelimiters;
			if( occuredDelimiters == n )
			{
				delimPtr = &bufferPtr[i];
				break;
			}
			goAtEndOfDelimiterTrail( &i );
		//	moveIfRequiredPastDelimiter( &i );
		}		
	}
	return delimPtr;
}

char* Tokenizer::goAtLastNthDelimiter( int startingOffset , int n )
{
	int i = startingOffset;
	if( bufferPtr == NULL )
		throw runtime_error( (char *)"Tokenizer::goAtLastNthDelimiter() : My buffer is NULL, how am I supposed to search for delimiters...?");
	if( startingOffset < 0 || startingOffset > bufferSize )
		throw runtime_error( (char *)"Tokenizer::goAtLastNthDelimiter() : Specified starting offset is invalid (either negative or bigger than the size of the buffer...?");
	if( n < 0 )
		throw runtime_error( (char *)"Tokenizer::goAtLastNthDelimiter() : Delimiter number needs to be a positive integer...?");
	//skipping all trailing delimiters;
	goAtEndOfDelimiterTrail( &i );
	if( n == 0 )
		return (bufferPtr+i);
	moveIfRequiredPastDelimiter( &i );
	char *delimPtr = NULL;
	int occuredDelimiters = 0;
	for( ; i < bufferSize ; ++i )
	{
		if( isDelimiter( bufferPtr[i] ) )
		{
			++occuredDelimiters;
			goAtEndOfDelimiterTrail( &i );
			if( occuredDelimiters == n )
			{
				delimPtr = &bufferPtr[i];
				break;
			}
		}		
	}
	return delimPtr;
}

char* Tokenizer::goAfterFirstNthDelimiter( int startingOffset , int n )
{
	int offSet;
	if( bufferPtr == NULL )
		throw runtime_error( (char *)"Tokenizer::goAfterFirstNthDelimiter() : My buffer is NULL, how am I supposed to search for delimiters...?");
	if( startingOffset < 0 || startingOffset > bufferSize - 1 )
		throw runtime_error( (char *)"Tokenizer::goAfterFirstNthDelimiter() : Specified starting offset is invalid (either negative or bigger than the size of the buffer...?");
	if( n < 0 )
		throw runtime_error( (char *)"Tokenizer::goAfterFirstNthDelimiter() : Delimiter number needs to be a positive integer...?");
	char *afterDelimPtr;
	if( ( afterDelimPtr = goAtFirstNthDelimiter( startingOffset , n ) ) != NULL )
	{
		if( isDelimiter( *afterDelimPtr ) )
		{
			offSet = afterDelimPtr - bufferPtr + 1;
			if( offSet >= bufferSize )
				afterDelimPtr = NULL;
			else
				++afterDelimPtr;
		}
	}
	else
		cout << "go After First Nth Delimiter returned a NULL pointer..." << endl;
	return afterDelimPtr;
}

char* Tokenizer::goAfterLastNthDelimiter( int startingOffset , int n )
{
	int offSet;
	if( bufferPtr == NULL )
		throw runtime_error( (char *)"Tokenizer::goAfterLastNthDelimiter() : My buffer is NULL, how am I supposed to search for delimiters...?");
	if( startingOffset < 0 || startingOffset > bufferSize - 1 )
		throw runtime_error( (char *)"Tokenizer::goAfterLastNthDelimiter() : Specified starting offset is invalid (either negative or bigger than the size of the buffer...?");
	if( n < 0 )
		throw runtime_error( (char *)"Tokenizer::goAfterLastNthDelimiter() : Delimiter number needs to be a positive integer...?");
	char *afterDelimPtr;
	if( ( afterDelimPtr = goAtLastNthDelimiter( startingOffset , n ) ) != NULL )
	{
		if( isDelimiter( *afterDelimPtr ) )
		{
			offSet = afterDelimPtr - bufferPtr + 1;
		//	cout << "after delim ptr points to:|" << afterDelimPtr << "| and offset is:" << offSet << endl;
			if( offSet >= bufferSize )
				afterDelimPtr = NULL;
			else
				++afterDelimPtr;
		}
	}
	else
		cout << "go After Last Nth Delimiter returned a NULL pointer..." << endl;
	return afterDelimPtr;
}

void Tokenizer::goAtEndOfDelimiterTrail( int* curIndex )
{
	if( *curIndex < bufferSize && isDelimiter( bufferPtr[*curIndex] ) )
	{
		while( *curIndex < bufferSize && isDelimiter( bufferPtr[*curIndex] ) )
			++(*curIndex);
		if( !isDelimiter( bufferPtr[*curIndex] ) )
			--(*curIndex);
	}
}

void Tokenizer::moveIfRequiredPastDelimiter( int* curIndex )
{
	if( *curIndex < bufferSize - 1 && isDelimiter( bufferPtr[*curIndex] ) )
		++(*curIndex);
}

char* EfficientTokenizer::getNthTokenPtr( int n )
{
	int offSet;
	if( n <= 0 )
		throw runtime_error( (char *)"EfficientTokenizer::getNthTokenPtrs() : Invalid token No. specified. Has to be greater or equal to 1...!");
	if( bufferPtr == NULL )
		throw runtime_error( (char *)"EfficientTokenizer::getNthTokenPtrs() : My buffer is NULL, how am I supposed to search for the n-th token...?");
	revertEndPtrVal();
	if( ( tokenStartPtr = goAfterLastNthDelimiter( 0 , n - 1 ) ) == NULL )
		return tokenStartPtr;
	offSet = tokenStartPtr - bufferPtr + 1;
//	cout << "Edw to offset einai:" << offSet << endl;
	if( isDelimiter( bufferPtr[offSet] ) || offSet == bufferSize )
		tokenEndPtr = bufferPtr + offSet;
	else
	{
		tokenEndPtr = goAtFirstNthDelimiter( offSet , 1 );
		if( tokenEndPtr == NULL )
		{
			tokenEndPtr = bufferPtr + bufferSize;
	//		cout << "I don't think this is possible ....offset = " << offSet << " and bufferSize = " << bufferSize << endl;
	//		return NULL;
		}
	}
//	cout << "and token end ptr points to:|" << tokenEndPtr << "|" << endl;
	if( isDelimiter( *tokenEndPtr ) )
		saveEndPtrVal();
	return tokenStartPtr;
}

char* SafeTokenizer::getNthTokenPtr( int n )
{	//implement this crap later-on....
	if( n <= 0 )
		throw runtime_error( (char *)"SafeTokenizer::getNthTokenPtr() : Invalid token No. specified. Has to be greater or equal to 1...!");
	if( bufferPtr == NULL )
		throw runtime_error( (char *)"SafeTokenizer::getNthTokenPtr() : My buffer is NULL, how am I supposed to search for the n-th token...?");
	return NULL;
}

