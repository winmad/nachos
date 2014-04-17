#include "syscall.h"

int main()
{
	int f = creat("lalala.txt");
	int i;
	for (i = 0; i < 10000000; i++);
	close(f);
	return 0;
}