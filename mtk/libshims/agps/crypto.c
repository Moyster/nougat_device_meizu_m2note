#include <malloc.h>

void CRYPTO_free(void *str) {
	    free(str);
}

void *CRYPTO_malloc(int num, const char *file, int line) {
		    return malloc(num);
}

void CRYPTO_lock(int mode, int type, const char *file, int line) {}
