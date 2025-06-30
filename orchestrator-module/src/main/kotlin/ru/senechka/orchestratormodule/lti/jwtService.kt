package ru.senechka.orchestratormodule.lti

import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.SignedJWT
import org.springframework.stereotype.Service
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.*

@Service
class JwtService {

    private val keyPair: KeyPair = generateKeyPair()
    private val rsaJWK: RSAKey = generateRsaJwk(keyPair)

    private fun generateKeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048)
        return generator.generateKeyPair()
    }

    private fun generateRsaJwk(keyPair: KeyPair): RSAKey {
        return RSAKey.Builder(keyPair.public as RSAPublicKey)
            .privateKey(keyPair.private as RSAPrivateKey)
            .keyID(UUID.randomUUID().toString())
            .algorithm(com.nimbusds.jose.Algorithm.parse("RS256"))
            .build()
    }

    fun getPublicJwk(): Map<String, Any> {
        return rsaJWK.toPublicJWK().toJSONObject()
    }

    fun getPrivateKey(): RSAPrivateKey {
        return rsaJWK.toRSAPrivateKey()
    }

    fun getKeyId(): String {
        return rsaJWK.keyID
    }
    fun verifyIdToken(idToken: String, publicKey: RSAPublicKey): Boolean {
        val signedJWT = SignedJWT.parse(idToken)
        val verifier = RSASSAVerifier(publicKey)

        return signedJWT.verify(verifier)
    }
}
