#include "syscall.h"

#define N 10

int main()
{
	int a[N][N], b[N][N], c[N][N], i, j, k;
	for(i = 0; i < N; i++)
		for(j = 0; j < N; j++)
			a[i][j] = i + j;
	for(i = 0; i < N; i++)
		for(j = 0; j < N; j++)
			b[i][j] = i - j;
	for(i = 0; i < N; i++)
		for(j = 0; j < N; j++)
			c[i][j] = 0;
	for(i = 0; i < N; i++)
		for(j = 0; j < N; j++)
			for(k = 0; k < N; k++)
				c[i][j] += a[i][k] * b[k][j];
	return 0;
}


