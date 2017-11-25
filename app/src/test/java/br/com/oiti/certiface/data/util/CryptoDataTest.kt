package br.com.oiti.certiface.data.util

import org.junit.Assert
import org.junit.Test

/**
 * Created by bzumpano on 19/11/17.
 */
class CryptoDataTest {

    private val appkey = "oKIZ1jjpRyXCDDNiR--_OPNGiNmraDZIuGE1rlUyZwOGJJzDtJR7BahJ4MqnobwetlmjXFsYFeze0eBRAGS2KWmjFUp08HYsv6pyI3KZklISJVmKJDSgfmkRmPaBR9ZJP3wtVWFDwNR9kS_vecameg"
    private val subject = CryptoData(appkey)

    @Test
    fun encrypt() {
        val data = "user,app,cpf,cpf,nome,name,nascimento,birthday"
        val expected = "nt8dru4FMCT62kDaGTK8RBY2FEE6n3GQpfvQaIiTSgP87gVmXnSb0b+Jbv5wLv4eF1YhRuCNAMj5LrxrFtavmg=="

        val result = subject.encrypt(data)

        Assert.assertEquals(expected, result)
    }

    @Test
    fun decrypt() {
        val data = "nt8dru4FMCT62kDaGTK8RBY2FEE6n3GQpfvQaIiTSgP87gVmXnSb0b+Jbv5wLv4eF1YhRuCNAMj5LrxrFtavmg=="
        val expected = "user,app,cpf,cpf,nome,name,nascimento,birthday"

        val result = subject.decrypt(data)

        Assert.assertEquals(expected, result)
    }

}
