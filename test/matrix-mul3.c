#include "syscall.h"

int main()
{
	int a[10][10], b[10][10], c[10][10], i, j, k;
	for(i = 0; i < 10; i++)
		for(j = 0; j < 10; j++)
			a[i][j] = i + j;
	for(i = 0; i < 10; i++)
		for(j = 0; j < 10; j++)
			b[i][j] = i - j;
	for(i = 0; i < 10; i++)
		for(j = 0; j < 10; j++)
			c[i][j] = 0;
	for(i = 0; i < 10; i++)
		for(j = 0; j < 10; j++)
			for(k = 0; k < 10; k++)
				c[i][j] += a[i][k] * b[k][j];
	i = 1 / 0;
	return 0;
}


