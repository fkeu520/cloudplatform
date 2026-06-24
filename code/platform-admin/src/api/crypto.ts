import { JSEncrypt } from 'jsencrypt'
import request from './request'

// F2: 公钥缓存加入 TTL，防止服务端重启后密钥变化导致加密失败
let publicKey: string | null = null
let publicKeyFetchedAt: number = 0
const CACHE_TTL_MS = 5 * 60 * 1000 // 5 分钟

/**
 * 获取 RSA 公钥（带 TTL 缓存）
 */
async function getPublicKey(): Promise<string> {
  const now = Date.now()
  if (publicKey && (now - publicKeyFetchedAt) < CACHE_TTL_MS) {
    return publicKey
  }
  // TTL 过期或未缓存 → 重新拉取
  try {
    const res: any = await request.get('/auth/public-key')
    publicKey = res.data?.publicKey || res.publicKey
    if (!publicKey) {
      throw new Error('获取公钥失败')
    }
    publicKeyFetchedAt = Date.now()
    return publicKey
  } catch (e) {
    console.error('获取RSA公钥失败:', e)
    // TTL 未过期但请求失败时，仍可使用旧缓存（不影响已缓存的场景）
    if (publicKey) return publicKey
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
 * 清除缓存的公钥（强制下次重新获取）
 */
export function clearPublicKeyCache(): void {
  publicKey = null
  publicKeyFetchedAt = 0
}
