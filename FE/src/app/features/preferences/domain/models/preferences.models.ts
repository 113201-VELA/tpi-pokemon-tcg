export interface UpdateNicknameRequest {
  nickname: string;
}

export interface UpdatePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface UpdateNicknameResponse {
  id: string;
  username: string;
  nickname: string;
  email: string;
  token: string | null;
}
