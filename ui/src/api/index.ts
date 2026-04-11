import {axiosInstance} from '@halo-dev/api-client'
import {CategoryV1alpha1Api, SentenceV1alpha1Api} from './generated'

const categoryCoreApiClient = {
  category: new CategoryV1alpha1Api(undefined, "", axiosInstance),
};
const sentenceCoreApiClient = {
  sentence: new SentenceV1alpha1Api(undefined, "", axiosInstance),
};

export {categoryCoreApiClient, sentenceCoreApiClient};


