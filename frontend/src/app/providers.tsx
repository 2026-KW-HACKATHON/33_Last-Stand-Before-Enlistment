"use client";

import { useMemo, type ReactNode } from "react";
import { usePathname, useRouter } from "next/navigation";
import { ApiClientProvider } from "../lib/api/provider";
import type { ApiClient } from "../lib/api/types";
import { destinationFromPathname, type SessionState } from "../lib/navigation";
import { LoginProvider } from "../features/auth/provider";
import type { OtpLoginService } from "../features/auth/contracts";
import { SignupProvider } from "../features/signup/provider";
import type { SignupEditors, SignupService } from "../features/signup/contracts";

import { ProfileProvider } from "../features/profile/provider";
import type { ProfileService } from "../features/profile/contracts";
import { NeighborProvider } from "../features/neighbor/provider";
import type { NeighborService } from "../features/neighbor/model";
import { InstitutionProvider } from "../features/institution/provider";
import type { InstitutionService } from "../features/institution/model";
import { MyPageProvider } from "../features/my-page/provider";
import type { ActivityService,MyMenuHandler } from "../features/my-page/model";
import { SettingsNavigation } from "../features/settings/SettingsNavigation";
import type { SettingsMenuHandler } from "../features/settings/model";
import { InterestRegionsHost } from "../features/interest-regions/InterestRegionsHost";
import type { InterestRegionService } from "../features/interest-regions/model";
import { InterestKeywordsHost } from "../features/interest-keywords/InterestKeywordsHost";
import type { InterestKeywordService } from "../features/interest-keywords/model";
import type { NotificationService, NotificationTargetHandler } from "../features/notifications/model";
import { PushPreferenceHost } from "../features/notification-settings/PushPreferenceHost";
import type { PushPreferenceService } from "../features/notification-settings/model";
import type { AccountInfoService } from "../features/account-info/model";
import type { EmailChangeRenderer } from "../features/account-info/AccountInfoScreen";
import { EmailChangeScreen } from "../features/email-change/EmailChangeScreen";
import type { EmailChangeService } from "../features/email-change/contracts";
import { LogoutSessionProvider } from "../features/logout/provider";
import type { CurrentDeviceLogoutService } from "../features/logout/model";
import { WithdrawalSessionProvider } from "../features/withdrawal/provider";
import type { WithdrawalService } from "../features/withdrawal/model";
import { PersonalListsProvider } from "../features/personal-lists/provider";
import type { PersonalListsService } from "../features/personal-lists/model";
import type { PersonalDetailRenderer } from "../features/personal-lists/PersonalListsScreen";
import { profileSignupEditors } from "../features/profile/editors";

/** Real adapters/client are injected by Integration; no production Mock or guessed base URL. */
export function AppProviders({ children, session, retrySession, apiClient, loginService, signupService, signupEditors, profileService, neighborService, institutionService, activityService, interestRegionService, interestKeywordService, onMyMenu, onSettingsMenu, notificationService, onNotificationTarget, pushPreferenceService, accountInfoService, emailChangeService, renderEmailChange, logoutService, withdrawalService, personalListsService, renderPersonalDetail, subjectKey = null }: {
  children: ReactNode; personalListsService?: PersonalListsService; renderPersonalDetail?: PersonalDetailRenderer; withdrawalService?: WithdrawalService; logoutService?: CurrentDeviceLogoutService; accountInfoService?: AccountInfoService; emailChangeService?: EmailChangeService; renderEmailChange?: EmailChangeRenderer; session?: SessionState; retrySession?: () => void | Promise<void>; apiClient?: ApiClient; loginService?: OtpLoginService; signupService?: SignupService; signupEditors?: SignupEditors; profileService?: ProfileService; neighborService?: NeighborService; institutionService?: InstitutionService; activityService?: ActivityService; interestRegionService?: InterestRegionService; interestKeywordService?: InterestKeywordService; pushPreferenceService?: PushPreferenceService; notificationService?: NotificationService; onNotificationTarget?: NotificationTargetHandler; onMyMenu?: MyMenuHandler; onSettingsMenu?: SettingsMenuHandler; subjectKey?: string | null;
}) {
  const router = useRouter();
  const pathname = usePathname();
  const currentDestination = useMemo(() => destinationFromPathname(pathname), [pathname]);
  return <LogoutSessionProvider session={session} retry={retrySession} subjectKey={subjectKey} service={logoutService}>
    <WithdrawalSessionProvider subjectKey={subjectKey} service={withdrawalService}><PushPreferenceHost subjectKey={subjectKey} service={pushPreferenceService}><InterestKeywordsHost subjectKey={subjectKey} service={interestKeywordService}><PersonalListsProvider subjectKey={subjectKey} service={personalListsService}><SettingsNavigation renderPersonalDetail={renderPersonalDetail} accountInfoService={accountInfoService} renderEmailChange={renderEmailChange ?? (request => <EmailChangeScreen key={subjectKey} request={request} account={accountInfoService ?? null} service={emailChangeService ?? null} subjectKey={subjectKey}/>)} notificationService={notificationService} onNotificationTarget={onNotificationTarget} subjectKey={subjectKey} onMenu={onSettingsMenu} currentDestination={currentDestination} onNavigate={(href, replace) => replace ? router.replace(href) : router.push(href)}>
      <LoginProvider service={loginService} subjectKey={subjectKey}>
        <InterestRegionsHost subjectKey={subjectKey} service={interestRegionService}><MyPageProvider subjectKey={subjectKey} service={activityService} onMenu={onMyMenu}><InstitutionProvider subjectKey={subjectKey} service={institutionService}><NeighborProvider subjectKey={subjectKey} service={neighborService}><ProfileProvider service={profileService}><SignupProvider service={signupService} editors={signupEditors ?? profileSignupEditors}>
          {apiClient ? <ApiClientProvider client={apiClient}>{children}</ApiClientProvider> : children}
        </SignupProvider></ProfileProvider></NeighborProvider></InstitutionProvider></MyPageProvider></InterestRegionsHost>
      </LoginProvider>
    </SettingsNavigation></PersonalListsProvider></InterestKeywordsHost></PushPreferenceHost></WithdrawalSessionProvider>
  </LogoutSessionProvider>;
}
