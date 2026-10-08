"use client";
import type { FeatureServices } from "./index";
import { PostHost } from "./PostHost";
import type { BookmarkDetailRenderer } from "../bookmarks/BookmarksScreen";
import type { PersonalDetailRenderer } from "../personal-lists/PersonalListsScreen";
/** Props for A's existing AppProviders; root file ownership remains with A. */
export function featureProviderProps(services: FeatureServices) {
  const renderBookmarkDetail: BookmarkDetailRenderer = request => <PostHost key={request.postId} services={services} postId={request.postId} bookmarkBinding={request.bookmark} onReturn={request.onReturn} />;
  const renderPersonalDetail: PersonalDetailRenderer = (postId, onReturn) => <PostHost key={postId} services={services} postId={postId} onReturn={onReturn} />;
  return {
    profileService: services.profileService, neighborService: services.neighborService,
    institutionService: services.institutionService, adoptionService: services.adoptionService,
    bookmarkService: services.bookmarkService, personalListsService: services.personalListsService,
    myVotesService: services.myVotesService, renderBookmarkDetail, renderPersonalDetail,
  };
}
