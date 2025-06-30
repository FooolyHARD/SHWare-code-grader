package ru.senechka.orchestratormodule.lti

import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.shaded.gson.internal.bind.TypeAdapters.URL
import com.nimbusds.jwt.JWTParser
import com.nimbusds.jwt.SignedJWT
import org.springframework.stereotype.Service
import java.net.URL
import java.security.interfaces.RSAPublicKey

@Service
class LtiLaunchService {

    fun verifyIdToken(idToken: String, jwksUrl: String): Boolean {
        val signedJWT = SignedJWT.parse(idToken)

        val jwkSet = JWKSet.load(URL(jwksUrl))
        val kid = signedJWT.header.keyID

        val jwk = jwkSet.getKeyByKeyId(kid)?.toRSAKey()
            ?: throw IllegalArgumentException("Public key with kid=$kid not found")

        val verifier = RSASSAVerifier(jwk.toRSAPublicKey())

        return signedJWT.verify(verifier)
    }

    fun extractClaims(idToken: String): Map<String, Any> {
        val signedJWT = SignedJWT.parse(idToken)
        return signedJWT.jwtClaimsSet.claims
    }
}
