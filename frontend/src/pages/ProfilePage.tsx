import { useEffect, useRef, useState } from 'react';
import { LogOut, RefreshCw, Save, Send, Smartphone, Upload } from 'lucide-react';
import { TopNav } from '../components/TopNav.tsx';
import { isAuthRequiredError } from '../lib/api.ts';
import { sendSms, storeAuthTokens } from '../lib/auth.ts';
import type { AuthTokens } from '../lib/auth.ts';
import {
  changeUserPhone,
  fetchUserProfile,
  updateUserProfile,
  uploadUserAvatar,
} from '../lib/dashboard.ts';
import type { UserProfile } from '../types/dashboard.ts';

type ProfilePageProps = {
  onAuthRequired: () => void;
  onAuthUpdate: (tokens: AuthTokens) => void;
  onLogout: () => void;
};

const maxAvatarSize = 5 * 1024 * 1024;

function maskPhone(phone: string) {
  return /^1[3-9]\d{9}$/.test(phone) ? `${phone.slice(0, 3)}****${phone.slice(7)}` : phone;
}

export function ProfilePage({ onAuthRequired, onAuthUpdate, onLogout }: ProfilePageProps) {
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const phoneTimerRef = useRef<number | null>(null);
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [nickname, setNickname] = useState('');
  const [newPhone, setNewPhone] = useState('');
  const [phoneCode, setPhoneCode] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [sendingPhoneCode, setSendingPhoneCode] = useState(false);
  const [changingPhone, setChangingPhone] = useState(false);
  const [phoneCountdown, setPhoneCountdown] = useState(0);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  async function loadProfile() {
    setLoading(true);
    setError('');
    setNotice('');
    try {
      const nextProfile = await fetchUserProfile();
      setProfile(nextProfile);
      setNickname(nextProfile.nickname || '');
    } catch (loadError) {
      if (isAuthRequiredError(loadError)) {
        onAuthRequired();
        return;
      }
      setError(loadError instanceof Error ? loadError.message : '用户资料加载失败');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadProfile();
  }, []);

  useEffect(() => () => {
    if (phoneTimerRef.current !== null) {
      window.clearInterval(phoneTimerRef.current);
    }
  }, []);

  useEffect(() => {
    if (!error || !profile) return undefined;
    const timer = window.setTimeout(() => setError(''), 1800);
    return () => window.clearTimeout(timer);
  }, [error, profile]);

  useEffect(() => {
    if (!notice) return undefined;
    const timer = window.setTimeout(() => setNotice(''), 1800);
    return () => window.clearTimeout(timer);
  }, [notice]);

  async function handleSaveNickname() {
    const trimmed = nickname.trim();
    if (trimmed.length < 2 || trimmed.length > 16) {
      setError('昵称长度需要为 2 到 16 个字符');
      return;
    }

    setSaving(true);
    setError('');
    setNotice('');
    try {
      const nextProfile = await updateUserProfile(trimmed);
      setProfile(nextProfile);
      setNickname(nextProfile.nickname);
      setNotice('昵称已更新');
    } catch (saveError) {
      if (isAuthRequiredError(saveError)) {
        onAuthRequired();
        return;
      }
      setError(saveError instanceof Error ? saveError.message : '昵称保存失败');
    } finally {
      setSaving(false);
    }
  }

  async function handleAvatarChange(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      setError('头像文件必须是图片');
      return;
    }
    if (file.size > maxAvatarSize) {
      setError('头像文件不能超过 5MB');
      return;
    }

    setUploading(true);
    setError('');
    setNotice('');
    try {
      const avatarUrl = await uploadUserAvatar(file);
      setProfile((prev) => prev ? { ...prev, avatarUrl } : prev);
      setNotice('头像已上传');
    } catch (uploadError) {
      if (isAuthRequiredError(uploadError)) {
        onAuthRequired();
        return;
      }
      setError(uploadError instanceof Error ? uploadError.message : '头像上传失败');
    } finally {
      setUploading(false);
    }
  }

  async function handleSendPhoneCode() {
    const trimmedPhone = newPhone.trim();
    if (!/^1[3-9]\d{9}$/.test(trimmedPhone)) {
      setNotice('');
      setError('请输入正确的新手机号');
      return;
    }

    setSendingPhoneCode(true);
    setError('');
    setNotice('');
    try {
      await sendSms(trimmedPhone);
      setNotice('验证码已发送到新手机号');
      setPhoneCountdown(60);
      if (phoneTimerRef.current !== null) {
        window.clearInterval(phoneTimerRef.current);
      }
      phoneTimerRef.current = window.setInterval(() => {
        setPhoneCountdown((prev) => {
          if (prev <= 1) {
            if (phoneTimerRef.current !== null) {
              window.clearInterval(phoneTimerRef.current);
              phoneTimerRef.current = null;
            }
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    } catch (sendError) {
      setError(sendError instanceof Error ? sendError.message : '验证码发送失败');
    } finally {
      setSendingPhoneCode(false);
    }
  }

  async function handleChangePhone() {
    const trimmedPhone = newPhone.trim();
    const trimmedCode = phoneCode.trim();
    if (!/^1[3-9]\d{9}$/.test(trimmedPhone)) {
      setNotice('');
      setError('请输入正确的新手机号');
      return;
    }
    if (!trimmedCode) {
      setNotice('');
      setError('请输入验证码');
      return;
    }

    setChangingPhone(true);
    setError('');
    setNotice('');
    try {
      const tokens = await changeUserPhone(trimmedPhone, trimmedCode);
      storeAuthTokens(tokens);
      onAuthUpdate(tokens);
      setProfile((prev) => prev ? { ...prev, phone: maskPhone(tokens.phone) } : prev);
      setNewPhone('');
      setPhoneCode('');
      setPhoneCountdown(0);
      if (phoneTimerRef.current !== null) {
        window.clearInterval(phoneTimerRef.current);
        phoneTimerRef.current = null;
      }
      setNotice('手机号已更换');
    } catch (changeError) {
      if (isAuthRequiredError(changeError)) {
        onAuthRequired();
        return;
      }
      setError(changeError instanceof Error ? changeError.message : '手机号更换失败');
    } finally {
      setChangingPhone(false);
    }
  }

  const avatarText = (profile?.nickname || profile?.phone || 'T').slice(0, 1).toUpperCase();

  return (
    <main className="app-page">
      <TopNav />
      <section className="page-section profile-section" aria-labelledby="profile-title">
        <div className="page-heading">
          <p>Profile</p>
          <h1 id="profile-title">用户管理</h1>
          <span>管理头像、昵称和当前登录状态</span>
        </div>

        {error ? (
          <div className={`state-block error-state${profile ? ' profile-feedback' : ''}`}>
            <span>{error}</span>
            {!profile ? (
              <button type="button" onClick={loadProfile}>
                <RefreshCw size={16} aria-hidden="true" />
                重试
              </button>
            ) : null}
          </div>
        ) : null}

        {notice ? <div className="state-block success-state profile-feedback">{notice}</div> : null}

        {loading ? <div className="state-block">正在加载用户资料...</div> : null}

        {profile ? (
          <div className="profile-layout">
            <div className="profile-avatar-panel">
              <div className="profile-avatar">
                {profile.avatarUrl ? <img src={profile.avatarUrl} alt="用户头像" /> : <span>{avatarText}</span>}
              </div>
              <button
                className="secondary-button"
                type="button"
                onClick={() => fileInputRef.current?.click()}
                disabled={uploading}
              >
                <Upload size={16} aria-hidden="true" />
                {uploading ? '上传中...' : '更换头像'}
              </button>
              <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                onChange={handleAvatarChange}
                hidden
              />
            </div>

            <div className="profile-fields">
              <label>
                <span>昵称</span>
                <input
                  value={nickname}
                  onChange={(event) => setNickname(event.target.value)}
                  maxLength={16}
                  placeholder="请输入昵称"
                />
              </label>
              <button className="primary-action" type="button" onClick={handleSaveNickname} disabled={saving}>
                <Save size={16} aria-hidden="true" />
                {saving ? '保存中...' : '保存昵称'}
              </button>

              <label>
                <span>手机号</span>
                <input value={profile.phone || '-'} readOnly />
              </label>

              <div className="profile-phone-card" aria-label="更改手机号">
                <label>
                  <span>新手机号</span>
                  <input
                    type="tel"
                    value={newPhone}
                    onChange={(event) => setNewPhone(event.target.value.replace(/\D/g, '').slice(0, 11))}
                    maxLength={11}
                    placeholder="请输入新手机号"
                    autoComplete="tel"
                  />
                </label>

                <label>
                  <span>验证码</span>
                  <div className="profile-code-row">
                    <input
                      value={phoneCode}
                      onChange={(event) => setPhoneCode(event.target.value.replace(/\D/g, '').slice(0, 6))}
                      maxLength={6}
                      placeholder="请输入验证码"
                      inputMode="numeric"
                    />
                    <button
                      className="secondary-button"
                      type="button"
                      onClick={handleSendPhoneCode}
                      disabled={sendingPhoneCode || phoneCountdown > 0 || changingPhone}
                    >
                      <Send size={16} aria-hidden="true" />
                      {sendingPhoneCode ? '发送中' : phoneCountdown > 0 ? `${phoneCountdown}s` : '获取验证码'}
                    </button>
                  </div>
                </label>

                <button
                  className="primary-action"
                  type="button"
                  onClick={handleChangePhone}
                  disabled={changingPhone}
                >
                  <Smartphone size={16} aria-hidden="true" />
                  {changingPhone ? '更换中...' : '更改手机号'}
                </button>
              </div>

              <button className="secondary-button logout-button" type="button" onClick={onLogout}>
                <LogOut size={16} aria-hidden="true" />
                退出登录
              </button>
            </div>
          </div>
        ) : null}
      </section>
    </main>
  );
}
