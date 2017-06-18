#include <openssl/ssl.h>

#define LOG_TAG "SSL_ctrl_stub"
#include <log/log.h>

long SSL_ctrl(SSL *ssl, int cmd, long larg, void *parg) {
    ALOGD("SSL_ctrl: ssl=%p cmd=%d larg=%ld parg=%p", ssl, cmd, larg, parg);
    return -1;
}

long SSL_CTX_ctrl(SSL_CTX *ctx, int cmd, long larg, void *parg) {
    ALOGD("SSL_CTX_ctrl: ctx=%p cmd=%d larg=%ld parg=%p", ctx, cmd, larg, parg);
    return -1;
}
