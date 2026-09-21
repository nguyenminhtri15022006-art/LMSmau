import React from 'react';
import { useParams } from 'react-router-dom';
import AnimatedPage from '../../../../../components/ui/AnimatedPage';
import LockedFeature from '../../../../../components/ui/LockedFeature';
import PreviewHeader from '../../components/teacher/PreviewImport/PreviewHeader';
import PreviewToolbar from '../../components/teacher/PreviewImport/PreviewToolbar';
import PreviewTable from '../../components/teacher/PreviewImport/PreviewTable';
import PreviewFooter from '../../components/teacher/PreviewImport/PreviewFooter';
import '../../styles/teacher/ImportTestCases/importTestCasesPage.css';

export default function PreviewImportPage() {
  const { courseId } = useParams();

  const testCases = [
    { id: 1, status: 'valid', input: '"Hello World"', output: '11', type: 'Public' },
    { id: 2, status: 'error', input: '', output: 'Error', type: 'Hidden', error: 'Missing field' },
    { id: 3, status: 'valid', input: '"EduLearn"', output: '8', type: 'Public' },
  ];

  return (
    <AnimatedPage>
      <LockedFeature featureName="Preview Import">
        <div className="coding-page import-test-cases-page">
          <PreviewHeader />
          <PreviewToolbar />
          <PreviewTable testCases={testCases} />
          <PreviewFooter courseId={courseId} />
        </div>
      </LockedFeature>
    </AnimatedPage>
  );
}
