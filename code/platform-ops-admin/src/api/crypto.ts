import { JSEncrypt } from 'jsencrypt'
import request from './request'

let publicKey: string | null = null

/**
 * 获取 RSA 公钥（带缓存）
 */
async function getPublicKey(): Promise<string> {
  if (publicKey) return publicKey
  try {
    const res: any = await request.get('/auth/public-key')
    publicKey = res.data?.publicKey || res.publicKey
    if (!publicKey) {
      throw new Error('获取公钥失败')
    }
    return publicKey
  } catch (e) {
    console.error('获取RSA公钥失败:', e)
    throw e
  }
}

/**
 * 使用 RSA 公钥加密数据
 */
export async function rsaEncrypt(data: string): Promise<string> {
  const key = await getPublicKey()
  const encrypt = new JSEncrypt()
  encrypt.setPublicKey(key)
  const encrypted = encrypt.encrypt(data)
  if (!encrypted) {
    throw new Error('RSA加密失败')
  }
  return encrypted
}

/**
 * 清除缓存的公钥（用于重新获取）
 */
export function clearPublicKeyCache(): void {
  publicKey = null
}
