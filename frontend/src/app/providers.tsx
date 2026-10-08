"use client";

import { useEffect, useRef, useMemo, type ReactNode } from "react";
import { usePathname, useRouter } from "next/navigation";
import { ApiClientProvider } from "../lib/api/provider";
import type { ApiClient } from "../lib/api/types";
import { destinationFromPathname, useNavigation, useSession, type SessionState } from "../lib/navigation";
import { AuthSessionProvider, type AuthSessionAdapter } from "../features/auth/session-adapter";
import { LoginProvider } from "../features/auth/provider";
import type { OtpLoginService } from "../features/auth/contracts";
import { SignupProvider } from "../features/signup/provider";
import type { SignupEditors, SignupService } from "../features/signup/contracts";

import { ProfileProvider } from "../features/profile/provider";
import type { ProfileService } from "../features/profile/contracts";
import { NeighborProvider } from "../features/neighbor/provider";
import type { NeighborService } from "../features/neighbor/model";
import { OfficerAgendasProvider, OfficerAgendasHost } from "../features/officer-agendas/OfficerAgendas";
import type { AdoptionService } from "../features/officer-agendas/model";
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
import { BookmarksProvider } from "../features/bookmarks/provider";
import type { BookmarkService } from "../features/bookmarks/model";
import type { BookmarkDetailRenderer } from "../features/bookmarks/BookmarksScreen";
import { MyVotesProvider } from "../features/my-votes/provider";
import type { MyVotesService } from "../features/my-votes/model";
import { PersonalListsProvider } from "../features/personal-lists/provider";
import type { PersonalListsService } from "../features/personal-lists/model";
import type { PersonalDetailRenderer } from "../features/personal-lists/PersonalListsScreen";
import { profileSignupEditors } from "../features/profile/editors";
import { PhoneFrame } from "../components/layout/PhoneFrame";

export type AppProvidersProps = {
  children: ReactNode; authScopeKey?: string | null; bookmarkService?: BookmarkService; renderBookmarkDetail?: BookmarkDetailRenderer; myVotesService?: MyVotesService; personalListsService?: PersonalListsService; renderPersonalDetail?: PersonalDetailRenderer; withdrawalService?: WithdrawalService; logoutService?: CurrentDeviceLogoutService; accountInfoService?: AccountInfoService; emailChangeService?: EmailChangeService; renderEmailChange?: EmailChangeRenderer; session?: SessionState; retrySession?: () => void | Promise<void>; sessionAdapter?: AuthSessionAdapter | null; apiClient?: ApiClient; loginService?: OtpLoginService; signupService?: SignupService; signupEditors?: SignupEditors; profileService?: ProfileService; neighborService?: NeighborService; institutionService?: InstitutionService; adoptionService?: AdoptionService; activityService?: ActivityService; interestRegionService?: InterestRegionService; interestKeywordService?: InterestKeywordService; pushPreferenceService?: PushPreferenceService; notificationService?: NotificationService; onNotificationTarget?: NotificationTargetHandler; onMyMenu?: MyMenuHandler; onSettingsMenu?: SettingsMenuHandler; subjectKey?: string | null;
};

/**
 * The app root owns exactly one session source. Integration injects a real
 * adapter here; without one, AuthSessionProvider deliberately remains loading.
 */
export function AppProviders({ children, session, sessionAdapter, ...props }: AppProvidersProps) {
  return <AuthSessionProvider adapter={sessionAdapter} session={session}><AppProvidersContent {...props}>{children}</AppProvidersContent></AuthSessionProvider>;
}

/** Real adapters/client are injected by Integration; no production Mock or guessed base URL. */
function AppProvidersContent({ children, authScopeKey, retrySession, apiClient, loginService, signupService, signupEditors, profileService, neighborService, institutionService, adoptionService, activityService, interestRegionService, interestKeywordService, onMyMenu, onSettingsMenu, notificationService, onNotificationTarget, pushPreferenceService, accountInfoService, emailChangeService, renderEmailChange, logoutService, withdrawalService, personalListsService, myVotesService, bookmarkService, renderBookmarkDetail, renderPersonalDetail, subjectKey = null }: Omit<AppProvidersProps, "session" | "sessionAdapter">) {
  const router = useRouter();
  const pathname = usePathname();
  const { session, retry } = useSession();
  const currentDestination = useMemo(() => destinationFromPathname(pathname), [pathname]);
  const page = apiClient ? <ApiClientProvider client={apiClient}>{children}</ApiClientProvider> : children;
  const content = <LogoutSessionProvider session={session} retry={retrySession ?? retry} subjectKey={subjectKey} service={logoutService}>
    <WithdrawalSessionProvider subjectKey={subjectKey} service={withdrawalService}><PushPreferenceHost subjectKey={subjectKey} service={pushPreferenceService}><InterestKeywordsHost subjectKey={subjectKey} service={interestKeywordService}><BookmarksProvider subjectKey={subjectKey} service={bookmarkService}><MyVotesProvider subjectKey={subjectKey} service={myVotesService}><PersonalListsProvider subjectKey={subjectKey} service={personalListsService}><SettingsNavigation renderBookmarkDetail={renderBookmarkDetail} renderPersonalDetail={renderPersonalDetail} accountInfoService={accountInfoService} renderEmailChange={renderEmailChange ?? (request => <EmailChangeScreen key={subjectKey} request={request} account={accountInfoService ?? null} service={emailChangeService ?? null} subjectKey={subjectKey}/>)} notificationService={notificationService} onNotificationTarget={onNotificationTarget} subjectKey={subjectKey} onMenu={onSettingsMenu} currentDestination={currentDestination} onNavigate={(href, replace) => replace ? router.replace(href) : router.push(href)}>
      <LoginProvider service={loginService} subjectKey={subjectKey}>
        <InterestRegionsHost subjectKey={subjectKey} service={interestRegionService}><MyPageProvider subjectKey={subjectKey} service={activityService} onMenu={onMyMenu}><InstitutionProvider subjectKey={subjectKey} service={institutionService}><OfficerAgendasProvider subjectKey={subjectKey} service={adoptionService}><OfficerAgendasHost destination={currentDestination}><NeighborProvider subjectKey={subjectKey} service={neighborService}><ProfileProvider service={profileService} subjectKey={authScopeKey ?? subjectKey}><SignupProvider subjectKey={authScopeKey ?? subjectKey} service={signupService} editors={signupEditors ?? profileSignupEditors}>
          <SessionScopeReset scope={authScopeKey ?? subjectKey} />{page}
        </SignupProvider></ProfileProvider></NeighborProvider></OfficerAgendasHost></OfficerAgendasProvider></InstitutionProvider></MyPageProvider></InterestRegionsHost>
      </LoginProvider>
    </SettingsNavigation></PersonalListsProvider></MyVotesProvider></BookmarksProvider></InterestKeywordsHost></PushPreferenceHost></WithdrawalSessionProvider>
  </LogoutSessionProvider>;
  return pathname.startsWith("/dev/") ? content : <PhoneFrame>{content}</PhoneFrame>;
}

function SessionScopeReset({ scope }: { scope: string | null }) {
  const navigation = useNavigation();
  const previous = useRef(scope);
  useEffect(() => {
    if (previous.current && previous.current !== scope) navigation.clear();
    previous.current = scope;
  }, [scope, navigation]);
  return null;
}
