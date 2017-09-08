config
======

To generate certs to give to Token, edit the `regenerate-certs` script to
use your domain and then run the script.
It updates `cert.pem` and `key.pem`.

(It doesn't update `trusted-certs.pem`; those are Token's certificates
for your server to trust.)