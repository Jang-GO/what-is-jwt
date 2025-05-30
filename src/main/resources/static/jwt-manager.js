// JWT 토큰 관리 클래스
class JWTManager {
    constructor() {
        this.baseURL = 'http://localhost:8080';
        this.init();
    }

    // 초기화
    init() {
        this.updateUI();
        this.checkTokenOnLoad();
    }

    // 페이지 로드 시 토큰 확인
    checkTokenOnLoad() {
        const accessToken = this.getAccessToken();
        if (accessToken) {
            this.validateToken();
        }
    }

    // 로컬 스토리지에서 토큰 가져오기
    getAccessToken() {
        return localStorage.getItem('accessToken');
    }

    getRefreshToken() {
        return localStorage.getItem('refreshToken');
    }

    // 토큰 저장
    saveTokens(accessToken, refreshToken) {
        localStorage.setItem('accessToken', accessToken);
        localStorage.setItem('refreshToken', refreshToken);
        this.updateTokenDisplay();
    }

    // 토큰 삭제
    clearTokens() {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        this.updateTokenDisplay();
    }

    // JWT 토큰 파싱 (페이로드 추출)
    parseJWT(token) {
        try {
            const base64Url = token.split('.')[1];
            const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
            const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
                return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
            }).join(''));
            return JSON.parse(jsonPayload);
        } catch (error) {
            console.error('JWT 파싱 오류:', error);
            return null;
        }
    }

    // 토큰 만료 확인
    isTokenExpired(token) {
        const payload = this.parseJWT(token);
        if (!payload) return true;

        const currentTime = Math.floor(Date.now() / 1000);
        return payload.exp < currentTime;
    }

    // 로그인
    async login() {
        const username = document.getElementById('loginUsername').value;
        const password = document.getElementById('loginPassword').value;

        try {
            const formData = new URLSearchParams();
            formData.append('username', username);
            formData.append('password', password);

            const response = await fetch(`${this.baseURL}/api/auth/login`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: formData
            });

            const data = await response.json();

            if (response.ok) {
                this.saveTokens(data.tokenResponse.accessToken, data.tokenResponse.refreshToken);
                this.showMessage('로그인 성공!', 'success');
                this.updateUI();
            } else {
                this.showMessage(`로그인 실패: ${data.message}`, 'error');
            }
        } catch (error) {
            this.showMessage(`로그인 오류: ${error.message}`, 'error');
        }
    }

    // 토큰 검증
    async validateToken() {
        const accessToken = this.getAccessToken();
        if (!accessToken) {
            this.showMessage('액세스 토큰이 없습니다.', 'error');
            return;
        }

        try {
            const response = await fetch(`${this.baseURL}/api/auth/validate`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${accessToken}`
                }
            });

            const data = await response.json();

            if (response.ok) {
                this.showMessage(data.message, 'success');
                this.updateUserInfo(accessToken);
            } else {
                this.showMessage(`토큰 검증 실패: ${data.message}`, 'error');
                if (response.status === 401) {
                    this.attemptTokenRefresh();
                }
            }
        } catch (error) {
            this.showMessage(`토큰 검증 오류: ${error.message}`, 'error');
        }
    }

    // 보호된 API 호출
    async callProtectedAPI() {
        const accessToken = this.getAccessToken();
        if (!accessToken) {
            this.showMessage('액세스 토큰이 없습니다.', 'error');
            return;
        }

        try {
            const response = await fetch(`${this.baseURL}/api/test/protected`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${accessToken}`
                }
            });

            if (response.ok) {
                const text = await response.text();
                this.showAPIResponse(text);
                this.showMessage('보호된 API 호출 성공!', 'success');
            } else {
                const data = await response.json();
                this.showMessage(`API 호출 실패: ${data.message}`, 'error');
                if (response.status === 401) {
                    this.attemptTokenRefresh();
                }
            }
        } catch (error) {
            this.showMessage(`API 호출 오류: ${error.message}`, 'error');
        }
    }

    // 토큰 갱신
    async refreshToken() {
        const refreshToken = this.getRefreshToken();
        if (!refreshToken) {
            this.showMessage('리프레시 토큰이 없습니다.', 'error');
            return;
        }

        try {
            const response = await fetch(`${this.baseURL}/api/auth/refresh`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ refreshToken: refreshToken })
            });

            const data = await response.json();

            if (response.ok) {
                this.saveTokens(data.accessToken, data.refreshToken);
                this.showMessage('토큰 갱신 성공!', 'success');
                this.updateUI();
            } else {
                this.showMessage(`토큰 갱신 실패: ${data.message}`, 'error');
                this.clearTokens();
                this.updateUI();
            }
        } catch (error) {
            this.showMessage(`토큰 갱신 오류: ${error.message}`, 'error');
        }
    }

    // 자동 토큰 갱신 시도
    async attemptTokenRefresh() {
        this.showMessage('액세스 토큰이 만료되었습니다. 자동으로 갱신을 시도합니다...', 'info');
        await this.refreshToken();
    }

    // 로그아웃
    async logout() {
        const refreshToken = this.getRefreshToken();

        if (refreshToken) {
            try {
                await fetch(`${this.baseURL}/api/auth/logout`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({ refreshToken: refreshToken })
                });
            } catch (error) {
                console.error('로그아웃 API 호출 오류:', error);
            }
        }

        this.clearTokens();
        this.showMessage('로그아웃되었습니다.', 'info');
        this.updateUI();
    }

    // 사용자 정보 API 호출
    async callUserInfoAPI() {
        const accessToken = this.getAccessToken();
        if (!accessToken) {
            this.showMessage('액세스 토큰이 없습니다.', 'error');
            return;
        }

        try {
            const response = await fetch(`${this.baseURL}/api/test/user-info`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${accessToken}`
                }
            });

            if (response.ok) {
                const data = await response.json();
                this.showAPIResponse(JSON.stringify(data, null, 2));
                this.showMessage('사용자 정보 API 호출 성공!', 'success');
            } else {
                const data = await response.json();
                this.showMessage(`API 호출 실패: ${data.message}`, 'error');
                if (response.status === 401) {
                    this.attemptTokenRefresh();
                }
            }
        } catch (error) {
            this.showMessage(`API 호출 오류: ${error.message}`, 'error');
        }
    }

// 게시글 작성 API 호출
    async callCreatePostAPI() {
        const accessToken = this.getAccessToken();
        if (!accessToken) {
            this.showMessage('액세스 토큰이 없습니다.', 'error');
            return;
        }

        try {
            const response = await fetch(`${this.baseURL}/api/test/create-post`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${accessToken}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    title: '테스트 게시글',
                    content: '이것은 JWT 인증을 통해 작성된 게시글입니다.'
                })
            });

            if (response.ok) {
                const data = await response.json();
                this.showAPIResponse(JSON.stringify(data, null, 2));
                this.showMessage('게시글 작성 API 호출 성공!', 'success');
            } else {
                const data = await response.json();
                this.showMessage(`API 호출 실패: ${data.message}`, 'error');
                if (response.status === 401) {
                    this.attemptTokenRefresh();
                }
            }
        } catch (error) {
            this.showMessage(`API 호출 오류: ${error.message}`, 'error');
        }
    }

// 관리자 API 호출
    async callAdminAPI() {
        const accessToken = this.getAccessToken();
        if (!accessToken) {
            this.showMessage('액세스 토큰이 없습니다.', 'error');
            return;
        }

        try {
            const response = await fetch(`${this.baseURL}/api/test/admin`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${accessToken}`
                }
            });

            if (response.ok) {
                const text = await response.text();
                this.showAPIResponse(text);
                this.showMessage('관리자 API 호출 성공!', 'success');
            } else if(response.status == 403) {
                const data = await response.text();
                this.showAPIResponse(data);
                this.showMessage(`${data.message}`, 'success');
            }else{
                const data = await response.json();
                this.showMessage(`API 호출 실패: ${data.message}`, 'error');
            }
        } catch (error) {
            this.showMessage(`API 호출 오류: ${error.message}`, 'error');
        }
    }

// 공개 API 호출 (토큰 없이)
    async callPublicAPI() {
        try {
            const response = await fetch(`${this.baseURL}/api/test/public`, {
                method: 'GET'
            });

            if (response.ok) {
                const text = await response.text();
                this.showAPIResponse(text);
                this.showMessage('공개 API 호출 성공! (인증 불필요)', 'success');
            } else {
                this.showMessage('공개 API 호출 실패', 'error');
            }
        } catch (error) {
            this.showMessage(`API 호출 오류: ${error.message}`, 'error');
        }
    }

    // 토큰 상태 확인
    checkTokenStatus() {
        const accessToken = this.getAccessToken();
        const refreshToken = this.getRefreshToken();

        if (!accessToken) {
            this.showMessage('액세스 토큰이 없습니다.', 'error');
            return;
        }

        const isExpired = this.isTokenExpired(accessToken);
        const payload = this.parseJWT(accessToken);

        if (payload) {
            const expireTime = new Date(payload.exp * 1000);
            const message = `토큰 상태: ${isExpired ? '만료됨' : '유효함'}\n만료 시간: ${expireTime.toLocaleString()}`;
            this.showMessage(message, isExpired ? 'error' : 'success');
        }
    }

    // UI 업데이트
    updateUI() {
        const accessToken = this.getAccessToken();
        const isLoggedIn = !!accessToken && !this.isTokenExpired(accessToken);

        document.getElementById('loginForm').classList.toggle('hidden', isLoggedIn);
        document.getElementById('loggedInMenu').classList.toggle('hidden', !isLoggedIn);
        document.getElementById('userInfo').classList.toggle('hidden', !isLoggedIn);

        const statusElement = document.getElementById('loginStatus');
        if (isLoggedIn) {
            statusElement.textContent = '로그인됨';
            statusElement.className = 'status success';
            this.updateUserInfo(accessToken);
        } else {
            statusElement.textContent = '로그인되지 않음';
            statusElement.className = 'status info';
        }

        this.updateTokenDisplay();
    }

    // 사용자 정보 업데이트
    updateUserInfo(accessToken) {
        const payload = this.parseJWT(accessToken);
        if (payload) {
            document.getElementById('username').textContent = payload.sub || '알 수 없음';
            document.getElementById('userRole').textContent = payload.role || '알 수 없음';
        }
    }

    // 토큰 표시 업데이트
    updateTokenDisplay() {
        const accessToken = this.getAccessToken();
        const refreshToken = this.getRefreshToken();

        document.getElementById('accessTokenDisplay').textContent =
            accessToken ? `${accessToken.substring(0, 50)}...` : '없음';
        document.getElementById('refreshTokenDisplay').textContent =
            refreshToken ? `${refreshToken.substring(0, 50)}...` : '없음';
    }

    // 메시지 표시
    showMessage(message, type) {
        const statusElement = document.getElementById('loginStatus');
        statusElement.textContent = message;
        statusElement.className = `status ${type}`;
    }

    // API 응답 표시
    showAPIResponse(response) {
        document.getElementById('apiResponse').textContent = response;
    }
}



// 전역 함수들 추가
function callUserInfoAPI() {
    jwtManager.callUserInfoAPI();
}

function callCreatePostAPI() {
    jwtManager.callCreatePostAPI();
}

function callAdminAPI() {
    jwtManager.callAdminAPI();
}

function callPublicAPI() {
    jwtManager.callPublicAPI();
}
// 전역 함수들 추가
function callUserInfoAPI() {
    jwtManager.callUserInfoAPI();
}

function callCreatePostAPI() {
    jwtManager.callCreatePostAPI();
}

function callAdminAPI() {
    jwtManager.callAdminAPI();
}

function callPublicAPI() {
    jwtManager.callPublicAPI();
}

// JWT 매니저 인스턴스 생성
const jwtManager = new JWTManager();

// 전역 함수들 (HTML에서 호출)
function login() {
    jwtManager.login();
}

function checkTokenStatus() {
    jwtManager.checkTokenStatus();
}

function callProtectedAPI() {
    jwtManager.callProtectedAPI();
}

function refreshToken() {
    jwtManager.refreshToken();
}

function logout() {
    jwtManager.logout();
}
