"use client";
import { useMemo, useState } from "react";
import { NavigationProvider, SessionProvider, type Destination } from "../../../lib/navigation";
import { InstitutionProvider } from "../../../features/institution/provider";
import { createMockInstitutionService, mockInstitutionSession, mockSubjectId, type InstitutionScenario } from "../../../features/institution/mock";
import { OfficerAgendasHost, OfficerAgendasProvider } from "../../../features/officer-agendas/OfficerAgendas";
import { createMockAdoptionService, mockOfficerAuthority, type OfficerScenario } from "../../../features/officer-agendas/mock";
export function OfficerPreview() {
  const [qualification, setQualification] = useState<InstitutionScenario>("valid");
  const [scenario, setScenario] = useState<OfficerScenario>("normal");
  const [destination, setDestination] = useState<Destination>({ id: "officerAgendas" });
  const institution = useMemo(() => createMockInstitutionService(qualification), [qualification]);
  const mock = useMemo(() => createMockAdoptionService(() => mockOfficerAuthority(qualification), scenario), [qualification, scenario]);
  return <><div className="mx-auto flex max-w-mobile flex-wrap gap-2 p-2 text-caption"><label>기관 <select value={qualification} onChange={e => setQualification(e.target.value as InstitutionScenario)}>{["valid", "expired", "none", "unknown", "error-retry"].map(value => <option key={value}>{value}</option>)}</select></label><label>조회/저장 <select value={scenario} onChange={e => setScenario(e.target.value as OfficerScenario)}>{["normal", "empty", "loading", "error", "save-error", "detail-error", "deleted"].map(value => <option key={value}>{value}</option>)}</select></label><button onClick={() => mock.scenario("normal")}>다음 재시도 성공</button></div>
    <SessionProvider session={mockInstitutionSession(qualification)}><NavigationProvider currentDestination={destination} onNavigate={href => { if (href === "/officer/agendas") setDestination({ id: "officerAgendas" }); else if (href.startsWith("/officer/agendas/")) setDestination({ id: "officerAgenda", params: { postId: decodeURIComponent(href.slice("/officer/agendas/".length)) } }); else setDestination({ id: "me" }); }}>
      <InstitutionProvider subjectKey={mockSubjectId} service={institution}><OfficerAgendasProvider subjectKey={mockSubjectId} service={mock.service}><OfficerAgendasHost destination={destination}><button onClick={() => setDestination({ id: "officerAgendas" })}>기관 업무로 이동</button></OfficerAgendasHost></OfficerAgendasProvider></InstitutionProvider>
    </NavigationProvider></SessionProvider>
  </>;
}
