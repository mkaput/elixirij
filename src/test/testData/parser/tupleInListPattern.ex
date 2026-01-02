# Minimal case: tuple destructuring in list pattern matching
pem_content = File.read!("certificate.pem")
[{:Certificate, der_bin, :not_encrypted}] = :public_key.pem_decode(pem_content)
der_bin
